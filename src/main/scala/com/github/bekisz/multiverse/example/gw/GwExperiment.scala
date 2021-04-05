package com.github.bekisz.multiverse.example.gw

import com.github.bekisz.multiverse.core.Implicits._
import com.github.bekisz.multiverse.core._
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
                    lambda: Parameter[Double] = ("1.0".toBD to "2.0".toBD by "0.05".toBD).map(_.toDouble),
                    maxPopulation: Parameter[Long] = Seq(100L)
                  ) extends Input

/**
 * These fields are the variables that we save from the trial instances for later analysis and will be the columns
 * in our SQL table (Dataset[GwOutput])
 */

case class GwOutput(lambda: Double,
                    maxPopulation: Long,
                    seedSurvivalChance: Double,
                    turn: Long,
                    isFinished: Boolean,
                    nrOfSeedNodes: Int,
                    trialUniqueId: String) extends Output

object GwOutput extends Output {
  def apply(t: GwTrial): GwOutput = new GwOutput(
    lambda = t.seedNode.lambdaForPoisson,
    maxPopulation = t.maxPopulation,
    seedSurvivalChance = if (t.isSeedDominant) 1.0 else 0.0,
    turn = t.turn(),
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

  def main(args: Array[String]): Unit = {

    val exp = new Experiment

    val trialOutput = exp.env
      .addSource(new IdGenerator).name("Id Generator")
      .flatMapWith(id => GwInput().createInputPermutations()).name("Creating Trial Inputs")
      .mapWith { case input: GwInput =>
        new GwTrial(input.maxPopulation,
          seedNode = new GwNode(input.lambda))
      }.name("Creating Trials")
      .flatMapWith {
        trial =>
          var outputList = List[GwOutput]()
          do {
            outputList = GwOutput(trial) :: outputList
          } while (trial.nextTurn())

          outputList = GwOutput(trial) :: outputList
          outputList.reverse
      }.name("Execute trial")

    exp.tableEnv.createTemporaryView("TrialOutputTable", trialOutput)
    /*
    exp.tableEnv.executeSql("CREATE CATALOG multiverse WITH('type' = 'jdbc','default-database' = 'postgres', " +
      " 'username' = 'multiverse', 'password' = 'multiverse', 'base-url' = 'jdbc:postgresql://localhost:5432/')")
    exp.tableEnv.executeSql("USE CATALOG multiverse") */
    val sql =
      "CREATE TABLE SurvivalByLambda " +
        "  (lambda DOUBLE, seedSurvivalChance DOUBLE, trials BIGINT, err DOUBLE, PRIMARY KEY (lambda) NOT ENFORCED) " +
        "   WITH('connector'= 'jdbc', 'url'= 'jdbc:postgresql://localhost:5432/replicator', " +
        "     'username'='multiverse', 'password'='multiverse', 'table-name' = 'SurvivalByLambda') " +
        "INSERT INTO SurvivalByLambda SELECT lambda, AVG(seedSurvivalChance), COUNT(*), " +
        "   ERROR(seedSurvivalChance,0.999) FROM TrialOutputTable GROUP BY lambda"
    exp.tableEnv.executeSql(sql)
    exp.env.execute("Galton-Watson Experiment")
    //exp.tableEnv.toRetractStream[Aggregation](aggregationTable)


    /*
      .filterWith { case (isUpdate, _) => isUpdate }.mapWith { case (_, piAggr) => piAggr }
      .addSink(result => println(s"Lambda : ${result.lambda} Seed Survival Chance : ${result.seedSurvivalChance} " +
        s" Count: ${result.samples}")).name("Galton-Watson Sink")


    val confidence = 0.99
    val inputDimNames = experiment.input.fetchDimensions().mkString(", ")
    val sqlSeedSurvivalChance: String = s"select $inputDimNames, count(seedSurvivalChance) as trials, " +
      "avg(seedSurvivalChance) as seedSurvivalChance, " +
      s"error(seedSurvivalChance, $confidence) as error " +
      s"from ${GwOutput.name}  where isFinished==true group by $inputDimNames order by $inputDimNames"

    experiment.outputRDD.toDS().createTempView(GwOutput.name)
    val seedSurvivalChanceDF = experiment.spark.sql(sqlSeedSurvivalChance)
    println("seedSurvivalChanceDF SQL query : " + sqlSeedSurvivalChance)
    val prolongTrialsTill = 50 //turns
    seedSurvivalChanceDF.show(100)

    seedSurvivalChanceDF.repartition(1)
      .write.format("csv").option("header", "true")
      .mode(SaveMode.Overwrite).save("output/GaltonWatson_seedSurvivalChance.csv")
    val sqlSeedPopulationByTurn: String = s"select lambda, turn, " +
      "avg(nrOfSeedNodes) as seedPopulation, " +
      s"error(nrOfSeedNodes, $confidence) as error " +
      s"from ${GwOutput.name}Prolonged where turn <= $prolongTrialsTill group by lambda, turn  order by lambda, turn"

    experiment.spark.table(GwOutput.name).retroActivelyProlongTrials(prolongTrialsTill)
      .createTempView(GwOutput.name + "Prolonged")
    val seedPopulationByTurnDF = experiment.spark.sql(sqlSeedPopulationByTurn)
    println("seedPopulationByTurnDF SQL query : " + sqlSeedPopulationByTurn)

     */

  }

}