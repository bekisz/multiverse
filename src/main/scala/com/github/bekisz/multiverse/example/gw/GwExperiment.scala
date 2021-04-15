package com.github.bekisz.multiverse.example.gw

import com.github.bekisz.multiverse.core.Implicits._
import com.github.bekisz.multiverse.core._
import com.github.bekisz.multiverse.utils.While
import org.apache.flink.streaming.api.scala.createTypeInformation
import org.apache.flink.streaming.api.scala.extensions.acceptPartialFunctions

/**
 * Input to our  <A HREF="https://en.wikipedia.org/wiki/Galton%E2%80%93Watson_process">Galton-Watson</A> Experiment.
 * Experiment runs all the combinations of these seedResourceAcquisitionFitness and totalResource variations. All fields should have type of
 * Parameter[T]. These parameters can be initialized with a Seq of T object or with the help implicit conversions with
 * T instances directly. These will be converted as a Seq with one element
 *
 * @param lambda        The seedResourceAcquisitionFitness of the Poisson distribution for the random generation of children replicators. It is also the
 *                      expected number of children of the seed replicators and its descendants
 * @param maxPopulation The theoretical work of Galton-Watson is limitless, and the seed node population grows
 *                      exponentially. For simulation though, it is necessary to define cut off point where we
 *                      declare our seed node as a survivor
 */
case class GwInput(
                    inputPlaneId: Parameter[Long],
                    lambda: Parameter[Double] = ("1.0".toBD to "2.0".toBD by "0.08".toBD).map(_.toDouble),
                    maxPopulation: Parameter[Long] = Seq(100L)

                  ) extends Input


trait OutputHasInputPlanes extends OutputHasLastTrialOutput {
  def inputPlaneId: Long
  def trialsInInputPlane :Long
}

trait OutputHasMultipleTurns extends Output {
  def turn: Long

  def isFinished: Boolean
}

trait OutputHasProlongedTurns extends OutputHasMultipleTurns with OutputHasLastTrialOutput {
  def prolongTrialsTill: Long = 50

  def isTurnProlonged: Boolean
}

trait OutputHasLastTrialOutput extends Output {
  def isLastTrialOutput: Boolean
}

/**
 * These fields are the variables that we save from the trial instances for later analysis and will be the columns
 * in our SQL table (Dataset[GwOutput])
 */

case class GwOutput(lambda: Double,
                    maxPopulation: Long,
                    seedSurvivalChance: Double,
                    turn: Long,
                    nrOfSeedNodes: Int,
                    trialUniqueId: String,
                    override val prolongTrialsTill: Long = 44,
                    isFinished: Boolean,
                    isTurnProlonged: Boolean,
                    inputPlaneId: Long,
                    trialsInInputPlane :Long,
                    isLastTrialOutput: Boolean = false) extends Output
  with OutputHasMultipleTurns
  with OutputHasInputPlanes
  with OutputHasProlongedTurns

object GwOutput extends Output {
  def apply(experimentInput: GwInput, i: GwInput, t: GwTrial): GwOutput = new GwOutput(
    lambda = i.lambda,
    maxPopulation = i.maxPopulation,
    inputPlaneId = i.inputPlaneId,
    trialsInInputPlane = experimentInput.multiplicity(),
    seedSurvivalChance = if (t.isSeedDominant) 1.0 else 0.0,
    turn = t.turn(),
    isTurnProlonged = false,
    isFinished = t.isFinished,
    nrOfSeedNodes = t.livingNodes.size,
    trialUniqueId = t.trialUniqueId
  )
}

//case class Aggregation(lambda: Double, seedSurvivalChance: Double, samples: Long, error: Double)

/**
 * Initiates our  <A HREF="https://en.wikipedia.org/wiki/Galton%E2%80%93Watson_process">Galton-Watson</A> Experiment
 *
 */
object GwExperiment {

  val confidence = 0.999
  val sinkWithClauseCommonPart =
    """'connector'= 'jdbc', 'url'= 'jdbc:postgresql://localhost:5432/replicator',
  'username'='multiverse', 'password'='multiverse'"""


  def main(args: Array[String]): Unit = {

    val exp = new Experiment

    val gwTrialOutput = exp.env
      .addSource(new IdGenerator).name("Generate InputPlaneIDs")
      .mapWith { inputPlaneId =>
        GwInput(inputPlaneId = inputPlaneId)
      }.name("Creating Input Planes")
      .flatMapWith { experimentInput =>
        experimentInput.createInputPermutations().map(trailInput => (experimentInput, trailInput))
      }.name("Creating Trial Inputs")
      .mapWith {
        case (experimentInput: GwInput, trailInput: GwInput) =>
          (experimentInput, trailInput, new GwTrial(trailInput.maxPopulation, seedNode = new GwNode(trailInput.lambda)))
      }.name("Creating Trials")
      .flatMapWith {
        case (experimentInput, trailInput, trial) => List(GwOutput(experimentInput, trailInput, trial)) ++
          While.withYield(!trial.isFinished) {
            GwOutput(experimentInput, trailInput, trial.nextTurn())
          }
      }.name("Running Trials")
      .mapWith {
        case output: OutputHasLastTrialOutput if output.isFinished => output.copy(isLastTrialOutput = true)
        case output => output
      }.name("Marking Last Trial Output")
      .flatMapWith {
        case output: OutputHasProlongedTurns =>
          if (output.isFinished)
            if (output.prolongTrialsTill > output.turn) {
              for (i <- output.turn to output.prolongTrialsTill) yield output.copy(turn = i, isTurnProlonged = i > output.turn,
                isLastTrialOutput = i == output.prolongTrialsTill)
            } else Seq(output.copy(isLastTrialOutput = true))
          else Seq(output)
        case output => Seq(output)
      }.name("Prolonging Trial Turns")
      .keyBy(_.inputPlaneId)
      .flatMapWithState {
        (trialOutput, state: Option[(Long, Seq[Output])]) => {
          trialOutput match {
            case trialOutput: OutputHasInputPlanes =>
              val (newFinishedTrials: Long, newEncounteredOutputs: Seq[GwOutput]) = state match {
                case Some((oldFinishedTrials: Long, oldEncounteredOutputs: Seq[OutputHasInputPlanes]))
                  => (oldFinishedTrials + (if (trialOutput.isLastTrialOutput) 1 else 0),
                    oldEncounteredOutputs :+ trialOutput)
                case None => (0L, Seq(trialOutput))
              }
              if (trialOutput.trialsInInputPlane == newFinishedTrials ) (newEncounteredOutputs, None)
              else (Seq.empty, Option((newFinishedTrials, newEncounteredOutputs)))

            case trialOutput => (Seq(trialOutput), state)
          }
        }
      }.name("Collating Input Plane")

    exp.tableEnv.createTemporaryView("GwTrialOutput", gwTrialOutput)
    /*
    exp.tableEnv.executeSql("CREATE CATALOG multiverse WITH('type' = 'jdbc','default-database' = 'postgres', " +
      " 'username' = 'multiverse', 'password' = 'multiverse', 'base-url' = 'jdbc:postgresql://localhost:5432/')")
    exp.tableEnv.executeSql("USE CATALOG multiverse") */

    s"""
       | CREATE TABLE `SurvivalByLambda`
       |    (lambda DOUBLE, seedSurvivalChance DOUBLE, trials BIGINT, err DOUBLE, PRIMARY KEY (lambda) NOT ENFORCED)
       |    WITH($sinkWithClauseCommonPart, 'table-name' = 'SurvivalByLambda');
       | INSERT INTO SurvivalByLambda SELECT lambda, AVG(seedSurvivalChance), COUNT(*),
       |   ERROR(seedSurvivalChance, $confidence) FROM GwTrialOutput
       |   WHERE isFinished=true AND isTurnProlonged=false
       |   GROUP BY lambda;
       | CREATE TABLE `SeedPopulationByTurn`
       |    (lambda DOUBLE, turn BIGINT,seedPopulation DOUBLE,  err DOUBLE, trials BIGINT,
       |     PRIMARY KEY (lambda, turn) NOT ENFORCED)
       |    WITH($sinkWithClauseCommonPart, 'table-name' = 'SeedPopulationByTurn');
       | INSERT INTO `SeedPopulationByTurn`  select lambda, turn,
       |      avg(CAST(nrOfSeedNodes AS DOUBLE)) as seedPopulation,
       |      error(nrOfSeedNodes, $confidence) as error,
       |       COUNT(*) as trials
       |      from GwTrialOutput where turn <= prolongTrialsTill group by lambda, turn"""
      .stripMargin.split(';').map(exp.tableEnv.executeSql)


    exp.env.execute("Galton-Watson Experiment")

  }

}