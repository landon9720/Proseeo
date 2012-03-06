package com.landonkuhn.proseeo.scriptmodel

import com.landonkuhn.proseeo._
import java.util.Date

import com.landonkuhn.proseeo._
import Util._
import Logging._

object ScriptStatementParser {

	def parseScriptStatement(statement:String):Statement = {
    parser.parse(parser.statement, statement) match {
      case parser.Success(statement, _) => statement
      case e:parser.NoSuccess => die("I don't understand [%s]".format(statement), e.msg)
    }
  }

	val parser = new Parser {
		def statement = (
			  "created" ~> by ~ at                ^^ { case by ~ at => Created(by, at) }
			| "ended" ~> by ~ at                  ^^ { case by ~ at => Ended(by, at) }
			| "say" ~> quotedText ~ by ~ at       ^^ { case quotedText ~ by ~ at => Say(quotedText, by, at) }
			| "set" ~> key ~ quotedText ~ by ~ at ^^ { case key ~ quotedText ~ by ~ at => Set(key, quotedText, by, at) }
			| route
		)
		def by = "by" ~> name
		def at = "@" ~> "[\\S]+".r ^? { case x if x.isDate => x.toDate }
		def route = "route" ~> "to" ~> actor ~ rep("then" ~> actor) ~ by ~ at ^^ { case name ~ then ~ by ~ at => RouteTo(name, then, by, at) }
	}
}

trait Statement
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
case class RouteTo(actor:Actor, then:Seq[Actor], by:String, at:Date) extends Statement {
	override def toString = "route to %s%s by %s @ %s".format(actor, then.map(" then " + _).mkString(""), by, at.format)
}