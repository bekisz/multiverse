package com.github.bekisz.multiverse.core.udf

import com.github.bekisz.multiverse.utils.{Logging, Statistics}
import org.apache.flink.table.functions.AggregateFunction

// mutable accumulator of structured type for the aggregate function


case class ErrorAccumulator(
                             var sum: Double = 0,
                             var sum2: Double = 0,
                             var count: Long = 0,
                             var conf: Double = 0.999
                           ) {

  def stdDev: Double = Math.sqrt(sum2.toDouble / count - mean * mean)

  def mean: Double = sum.toDouble / count

}

class Error extends AggregateFunction[java.lang.Double, ErrorAccumulator] with Logging {
  type JavaDouble = java.lang.Double

  override def createAccumulator(): ErrorAccumulator = ErrorAccumulator()

  override def getValue(acc: ErrorAccumulator): JavaDouble = {
    if (acc.count > 2) {
      val (low, _) = Statistics.confidenceInterval(acc.count, acc.mean, acc.stdDev, acc.conf)
      acc.mean - low
    } else {
      log.warn(s"The sample size is ${acc.count}. Confidence calculation is not possible with " +
        s"sample size less than two. Returning <Double.MaxValue> as the result of 'ERROR' UDAF.")
      Double.MaxValue
    }
  }

  def accumulate(acc: ErrorAccumulator, iValue: JavaDouble, conf: JavaDouble): Unit = {
    acc.count += 1
    acc.sum += iValue
    acc.sum2 += iValue * iValue
    acc.conf = conf
  }

  def retract(acc: ErrorAccumulator, iValue: JavaDouble, conf: JavaDouble): Unit = {
    acc.count -= 1
    acc.sum -= iValue
    acc.sum2 -= iValue * iValue
    acc.conf = conf
  }


  def merge(acc: ErrorAccumulator, it: java.lang.Iterable[ErrorAccumulator]): Unit = {
    val iter = it.iterator()
    while (iter.hasNext) {
      val a = iter.next()
      acc.count += a.count
      acc.sum += a.sum
      acc.sum2 += a.sum2
      acc.conf = a.conf
    }
  }

  def resetAccumulator(acc: ErrorAccumulator): Unit = {
    acc.count = 0L
    acc.sum = 0L
    acc.sum2 = 0L
    acc.conf = 0.999
  }

}
