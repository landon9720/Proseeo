package com.landonkuhn.proseeo

import java.util.Date

import Util._
import Logging._

object ScriptStatementParser {

	def parseScriptStatement(statement:String):Statement = {
		parser.parseAll(parser.statement, statement) match {
			case parser.Success(statement, _) => statement
			case e:parser.NoSuccess => die("I don't understand [%s]\n  %s".format(statement, e.msg))
		}
	}

	val parser = new Parser {
		def statement = (
			"created" ~> by ~ at ^^ {
				case by ~ at => Created(by, at)
			}
				| "ended" ~> by ~ at ^^ {
				case by ~ at => Ended(by, at)
			}
				| "say" ~> quotedText ~ by ~ at ^^ {
				case quotedText ~ by ~ at => Say(quotedText, by, at)
			}
				| "set" ~> key ~ quotedText ~ by ~ at ^^ {
				case key ~ quotedText ~ by ~ at => Set(key, quotedText, by, at)
			}
				| "delete" ~> key ~ by ~ at ^^ {
				case key ~ by ~ at => Delete(key, by, at)
			}
				| "route" ~> actors ~ by ~ at ^^ {
				case actors ~ by ~ at => Route(actors, by, at)
			}
			)

		def by = "by" ~> key

		def at = "@" ~> "[\\S]+".r ^? {
			case x if x.isDate => x.toDate
		}

		def actors = "(" ~> repsep(key, ",") <~ ")" ^^ {
			case actors => actors
		}
	}
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