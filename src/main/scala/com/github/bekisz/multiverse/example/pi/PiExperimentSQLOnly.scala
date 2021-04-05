package com.github.bekisz.multiverse.example.pi

import com.github.bekisz.multiverse.core.Experiment


/**
 * Runs a Pi approximation with Monte Carlo method via Flink's <B>SQL</B> API.
 *
 * Generates random points in a 2x2 box, centered in the origin.
 * The ratio of withinCircle points in the sample estimates Pi/4.
 * Rationale : The area of the 2x2 box is 4, the area of the 1 unit radius circle inside is 1*1*Pi = Pi
 * by definition. So the ratio of these two areas are :
 * Pi/4 = P(isWithinCircle)/1 => Pi = 4 * P(isWithinCircle)
 */

object PiExperimentSQLOnly {

  val confidence: Double = 0.999

  def main(args: Array[String]): Unit = {
    val exp = new Experiment

    val sql = s"""
                 | CREATE TEMPORARY TABLE `InputGenerator` (id BIGINT NOT NULL)
                 |     WITH ('connector' = 'datagen',  'rows-per-second' ='2147483647');
                 | CREATE FUNCTION PI_CORE AS 'com.github.bekisz.multiverse.example.pi.PiEstimatorCoreFunction';
                 | CREATE VIEW `PiOutput` AS SELECT PI_CORE(id) as estimatedPi FROM `InputGenerator`;
                 | CREATE VIEW `PiResult`
                 |    AS SELECT AVG(estimatedPi) AS estimatedPi, COUNT(*) AS trials, $confidence AS confidence,
                 |    ERROR(estimatedPi, $confidence) as err FROM `PiOutput`;
                 | CREATE TABLE `PiResultText` (text STRING) WITH('connector' = 'print');
                 | INSERT INTO `PiResultText` SELECT
                 |      CONCAT('\nThe empirical PI = ', CAST(estimatedPi AS VARCHAR),
                 |        ' +/-', CAST(err AS VARCHAR), ' with ', CAST(confidence*100 AS VARCHAR),'% confidence',
                 |        '\n  - # of trials = ', CAST(trials/1000000 AS VARCHAR), ' million',
                 |        '\n  - Distance from real PI = ', CAST((ABS(PI()- estimatedPi)) AS VARCHAR), '\n')
                 |   FROM `PiResult`
            """.stripMargin.split(';')
    //sql.map(println(_))
    sql.map(exp.tableEnv.executeSql(_))
  }
}
