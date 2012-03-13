package com.landonkuhn.proseeo

import Ansi._
import Util._

object Logging {

	def ok(s:String)    { println(s.split("\n").map("ok".white.bold.bggreen + " " + _).mkString("\n")) }
	def i(s:String)  { println(s.split("\n").map("   " + _).mkString("\n")) }
	def warn(s:String)  { System.err.println(s.split("\n").map("??".white.bold.bgyellow + " " + _).mkString("\n")) }
	def error(s:String) { System.err.println(s.split("\n").map("!?".white.bold.bgyellow + " " + _).mkString("\n")) }

	def die(s:String):Nothing = {
		throw new Dying(s)
	}

	def dye_for_real(ex:Exception):Nothing = {
		i(ex.getStackTraceString)
		error(ex.getMessage)
		sys.exit
	}

	type Dying = Exception
}