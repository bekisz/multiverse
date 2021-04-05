package com.github.bekisz.multiverse.example.pi

import com.github.bekisz.multiverse.core.{Experiment, IdGenerator}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions.acceptPartialFunctions

import scala.math.random

/**
 * Runs a Pi approximation with Monte Carlo method via Flink's <B>SQL</B> API.
 *
 * Generates random points in a 2x2 box, centered in the origin.
 * The ratio of withinCircle points in the sample estimates Pi/4.
 * Rationale : The area of the 2x2 box is 4, the area of the 1 unit radius circle inside is 1*1*Pi = Pi
 * by definition. So the ratio of these two areas are :
 * Pi/4 = P(isWithinCircle)/1 => Pi = 4 * P(isWithinCircle)
 */

object PiExperimentMixed2 {


  val confidence = 0.999
  def main(args: Array[String]): Unit = {

    val exp = new Experiment
    val piOutput = exp.env
      .addSource(new IdGenerator).name("Id Generator")
      .mapWith { _ =>
        val (x, y) = (random * 2 - 1, random * 2 - 1)
        PiTrialOutput(estimatedPi = if (x * x + y * y < 1) 4.0 else 0.0 )
      }.name("Inner or Outer Circle Random Trials")

    exp.tableEnv.createTemporaryView("PiOutput", piOutput)
    val piAggregationTable = exp.tableEnv.sqlQuery(s"""
                  | SELECT AVG(estimatedPi), COUNT(*), $confidence, ERROR(estimatedPi,$confidence)
                  |   FROM `PiOutput`
                  """.stripMargin)

    exp.tableEnv.toRetractStream[PiAggregation](piAggregationTable)
      .filterWith { case (isUpdate, _) => isUpdate }.mapWith { case (_, piAggr) => piAggr }
      .addSink(result => println(result.toString)).name("Pi Sink")
    exp.env.execute("Pi Estimation 2 with Flink Mixed DS/SQL APIs")

  }
}
