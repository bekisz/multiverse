package com.github.bekisz.multiverse.utils

trait HasMeasuredLifeTime {
  val t0 = java.lang.System.currentTimeMillis()
  def lifeTime() : Long = java.lang.System.currentTimeMillis() -t0
}
