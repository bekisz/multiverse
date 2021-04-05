package com.github.bekisz.multiverse.core

import com.github.bekisz.multiverse.utils.HasMeasuredLifeTime
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.EnvironmentSettings
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment

class Experiment extends HasMeasuredLifeTime {

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  val tableEnv = this.createTableEnv()
  // register function
  protected def createTableEnv(): StreamTableEnvironment = {
    val envSettings = EnvironmentSettings.newInstance()
      .useBlinkPlanner().inStreamingMode().build()


    val tableEnv = StreamTableEnvironment.create(this.env, envSettings)
    val configuration = tableEnv.getConfig.getConfiguration
    // local-global aggregation depends on mini-batch is enabled
    configuration.setString("table.exec.mini-batch.enabled", "true")
    configuration.setString("table.exec.mini-batch.allow-latency", "1s")
    configuration.setString("table.exec.mini-batch.size", "1000000")
    // enable two-phase, i.e. local-global aggregation
    configuration.setString("table.optimizer.agg-phase-strategy", "TWO_PHASE")
    tableEnv.createTemporarySystemFunction("ERROR", classOf[com.github.bekisz.multiverse.core.udf.Error])
    tableEnv
  }
}
