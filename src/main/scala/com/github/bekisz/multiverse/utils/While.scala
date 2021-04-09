package com.github.bekisz.multiverse.utils

object While {

 def withYield[T](cond : =>Boolean)(body : =>T) : Seq[T] = {
  @scala.annotation.tailrec
  def loop(previous : Seq[T]) : Seq[T] = if(cond) loop( previous :+ body) else previous
  if(cond) loop(List[T]():+ body ) else List[T]()
  }

  def main(args: Array[String]): Unit = {
    var i = 0L
    val listOfNumber = While.withYield(i<5) {
      i = i + 1
      i
    }
    println(listOfNumber.toString())
  }
}