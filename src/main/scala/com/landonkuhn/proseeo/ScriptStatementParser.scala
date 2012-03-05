package com.landonkuhn.proseeo

import com.landonkuhn.proseeo.Logging._
import util.parsing.combinator.RegexParsers
import java.util.Date
import Util._

object ScriptStatementParser {

	def parseScriptStatement(statement:String):Statement = {
    parser.parse(parser.statement, statement) match {
      case parser.Success(statement, _) => statement
      case e:parser.NoSuccess => die("I don't understand [%s]".format(statement), e.msg)
    }
  }

	val parser = new RegexParsers {
		def statement = (
			  "created" ~> by ~ at                ^^ { case by ~ at => Created(by, at) }
			| "say" ~> quotedText ~ by ~ at       ^^ { case quotedText ~ by ~ at => Say(quotedText, by, at) }
			| "set" ~> key ~ quotedText ~ by ~ at ^^ { case key ~ quotedText ~ by ~ at => Set(key, quotedText, by, at) }
		)
		def by         = "by" ~> "[a-zA-Z0-9]+".r   ^^ { case x => x }
		def at         = "@" ~> "[\\S]+".r          ^? { case x if x.isDate => x.toDate }
		def quotedText = "\"" ~> "[^\"]+".r <~ "\"" ^^ { case x => x }
		def key        = "[a-zA-Z0-9._]+".r
	}

	trait Statement
	case class Created(by:String, at:Date) extends Statement {
		override def toString = "created by %s @ %s".format(by, at.format)
	}
	case class Say(text:String, by:String, at:Date) extends Statement {
		override def toString = "say \"%s\" by %s @ %s".format(text, by, at.format)
	}
	case class Set(key:String, value:String, by:String, at:Date) extends Statement {
		override def toString = "set %s \"%s\" by %s @ %s".format(key, value, by, at.format)
	}
}