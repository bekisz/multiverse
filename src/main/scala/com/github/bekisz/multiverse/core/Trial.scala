package com.github.bekisz.multiverse.core

import java.util.UUID

/**
 * Common trait of all trials. Trials can be driven turn-by-turn till it gets to an isFinished=true state
 * or can be started by createOutputRDD where the control is given back to the framework after the trial is fully finished with
 * all of its turns.
 *
 * All Trials subclasses inherit an universal unique ID
 */
trait Trial extends Serializable  {

  private var _turn = 0L
  def hasNext:Boolean = !this.isFinished
  def isFinished: Boolean
  /**
   * @return turn number ~ time
   */
  def turn(): Long = _turn

  /**
   * Takes one turn
   * @return true if is has a next turn, false it was the final turn for this trial
   */
  def nextTurn() : Trial = {
    _turn +=1
    this
  }

  /**
   * Runs the trial with 1-many turns till it gets finished
   * @return this trial
   */

    def run() : Trial = {
      while(!this.nextTurn().isFinished) {}
      this
    }

  /**
   * The universally unique id of this trial
   */
  val trialUniqueId: String = UUID.randomUUID().toString
}
