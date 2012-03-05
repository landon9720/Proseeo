package com.landonkuhn.proseeo

import Ansi._
import Util._

object Logging {

	def ok(s:String) {
		println(s.white.bggreen.bold)
	}

	def debug(s:String) {
		// if (verbose) // later
		println(s.cyan)
	}

	def info(s:String) {
		println(s)
	}

	def error(s:String, sd:String) { error(s, Some(sd)) }
	def error(s:String, sd:Option[String] = None) {
		println(s.white.bold.bgred)
		for (sd <- sd) debug(sd.indent)
	}

	def die(s:String, sd:String): Nothing = die(s, Some(sd))
	def die(s:String, sd:Option[String] = None): Nothing = {
		error(s)
		for (sd <- sd) debug(sd.indent)
		if (doNotDie) sys.error(s)
    else sys.exit
	}

  var doNotDie = false
}