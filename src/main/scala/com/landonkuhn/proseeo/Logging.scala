package com.landonkuhn.proseeo

import Ansi._
import Util._

object Logging {

	def ok(s:String)    { println(s.split("\n").map("ok".white.bold.bggreen + " " + _).mkString("\n")) }
	def i(s:String)  { println(s.split("\n").map("   " + _).mkString("\n")) }
	def warn(s:String)  { println(s.split("\n").map("??".white.bold.bgyellow + " " + _).mkString("\n")) }
	def error(s:String) { println(s.split("\n").map("!?".white.bold.bgyellow + " " + _).mkString("\n")) }

	def die(s:String):Nothing = {
		error(s)
		if (exception_on_die) sys.error(s)
		else sys.exit
	}

  var exception_on_die = true
}