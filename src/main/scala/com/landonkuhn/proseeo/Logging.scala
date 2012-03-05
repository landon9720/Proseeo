package com.landonkuhn.proseeo

import Ansi._
import Implicits._

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

	def error(s:String, sd:String = "") {
		println(s.white.bold.bgred)
		if (sd != "") debug(sd.indent("  "))
	}

	def die(s:String, sd:String = ""): Nothing = {
		error(s)
		if (sd != "") debug(sd.indent("  "))
		sys.exit
	}
}