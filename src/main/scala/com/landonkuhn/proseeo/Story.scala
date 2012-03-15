package com.landonkuhn.proseeo

import java.util.Date
import Util._

trait Story {
	val name:String
	val plan:Option[String]
	val created:Option[Created]
	val ended:Option[Ended]
	val tail:Option[Statement]
	val document:Document // later think of something else to call this
	val route:RouteState
}

trait Statement {
	def by:String
	def at:Date
}

case class Created(by:String, at:Date) extends Statement {
	override def toString = "created by %s @ %s".format(by, at.format)
}

case class Ended(by:String, at:Date) extends Statement {
	override def toString = "ended by %s @ %s".format(by, at.format)
}

case class Say(text:String, by:String, at:Date) extends Statement {
	override def toString = "say \"%s\" by %s @ %s".format(text, by, at.format)
}

case class Set(key:String, value:String, by:String, at:Date) extends Statement {
	override def toString = "set %s \"%s\" by %s @ %s".format(key, value, by, at.format)
}

case class Delete(key:String, by:String, at:Date) extends Statement {
	override def toString = "delete %s by %s @ %s".format(key, by, at.format)
}

case class Route(actors:Seq[String], by:String, at:Date) extends Statement {
	override def toString = "route (%s) by %s @ %s".format(actors.mkString(", "), by, at.format)
}

case class RouteState(past:Seq[String], present:Option[String], future:Seq[String])
