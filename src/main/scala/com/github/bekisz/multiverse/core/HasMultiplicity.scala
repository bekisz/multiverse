package com.github.bekisz.multiverse.core

/**
 * Denotes a class that consists of a set of parameters/trials that can have multiple execution
 */
trait HasMultiplicity {
  def multiplicity(): Long
}
