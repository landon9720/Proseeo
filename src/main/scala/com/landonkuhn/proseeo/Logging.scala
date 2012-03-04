package com.landonkuhn.proseeo

import Ansi._

object Logging {

  def die(s:String) {
    error(s)
    sys.exit
  }

  def error(s:String) {
    println(s.white.bold.bgred)
  }

  def die(s:String, t:Throwable) {
    error(s, t)
    sys.exit
  }

  def error(s:String, t:Throwable) {
    println(s.white.bold.bgred)
    println("%s: %s\n%s".format(t.getClass.getName, t.getMessage, t.getStackTraceString.lines.mkString("  ", "\n  ", "")).yellow)
  }

  def info(s:String) {
    println(s)
  }

  def debug(s:String) {
    // if (verbose) // TODO
    println(s.cyan)
  }
}