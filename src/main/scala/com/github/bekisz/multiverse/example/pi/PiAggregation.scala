package com.github.bekisz.multiverse.example.pi

case class PiAggregation(empiricalPi: Double, count: Long,
                         confidence:Double, error:Double) {
  override def toString: String = {
    val warning = if (error < Math.abs(Math.PI - empiricalPi))
      "\\n  - Warning : Empirical Pi is outside of the Confidentiality Interval !" else ""
    s"Empirical PI = $empiricalPi +/-$error with ${confidence*100}% statistical confidence" +
      s"\n  - # of trials = ${count / (1 * 1000 * 1000)} million" +
      s"\n  - Distance from real PI = ${Math.abs(Math.PI - empiricalPi)}" +
      s"$warning"


  }
}
