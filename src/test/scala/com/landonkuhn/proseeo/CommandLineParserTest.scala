package com.landonkuhn.proseeo

import org.junit._
import org.junit.Assert._

import CommandLineParser._

class CommandLineParserTest {

  Logging.doNotDie = true

  @Test
  def help {
    parseCommandLine("help") match {
      case Help() =>
      case _ => fail
    }
  }

  @Test
  def init {
    parseCommandLine("init a b c") match {
      case Init("a b c") =>
      case _ => fail
    }
  }

  @Test
  def use {
    parseCommandLine("use 91bf1358bed94ff69d11242d8ec6fb5d") match {
      case Use("91bf1358bed94ff69d11242d8ec6fb5d") =>
      case _ => fail
    }
  }
}