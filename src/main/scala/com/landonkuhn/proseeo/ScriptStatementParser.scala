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
