package com.github.bekisz.multiverse.example.pi

import org.apache.flink.table.functions.ScalarFunction

import scala.math.random

class PiEstimatorCoreFunction extends ScalarFunction {

  // take any data type and return INT @DataTypeHint(inputGroup = InputGroup.ANY) o: AnyRef
  def eval(id: Long): Double = {
    val (x, y) = (random * 2 - 1, random * 2 - 1)
    if (x * x + y * y < 1) 4.0 else 0.0
  }
}

