package com.landonkuhn.proseeo

import org.junit._
import org.junit.Assert._

import CommandLineParser._

class CommandLineParserTest {

  Logging.doNotDie = true

  @Test
  def test1 {
    parseCommandLine("say foo bar") match {
      case Say("foo bar") =>
      case _ => fail
    }
  }
}