package com.landonkuhn.proseeo.cli

import org.junit._
import org.junit.Assert._

import CommandLineParser._
import com.landonkuhn.proseeo.Logging

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
		parseCommandLine("init a") match {
			case Init("a") =>
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