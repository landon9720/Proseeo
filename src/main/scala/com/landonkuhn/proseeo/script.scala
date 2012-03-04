package com.landonkuhn.proseeo

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical
import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import Logging._
import Files._

class Script(file: File) {

	def append(statement: Statement): Script = {
		statements += statement
		this
	}

	def save: Script = {
		write(file, statements.map(_.toString))
		this
	}

	private val parser = new StandardTokenParsers {

		def parseStatement(line:String):Statement = phrase(statement)(new lexical.Scanner(line)) match {
			case Success(statement, _) => statement
			case e:NoSuccess => die("I don't understand statement [%s] in file [%s]".format(line, file))
		}

		override val lexical = new StdLexical {
			reserved +=(
				"at",
				"by",
				"created",
				"set",
				"route", "to", "then",
				"say"
				)
			delimiters ++= List()

			import scala.util.parsing.input.CharArrayReader.EofCh

			override def token:Parser[Token] =
				(identChar ~ rep(identChar | digit) ^^ {
					case first ~ rest => processIdent(first :: rest mkString "")
				}
					| digit ~ rep(digit) ^^ {
					case first ~ rest => NumericLit(first :: rest mkString "")
				}
					| '\'' ~ rep(chrExcept('\'', '\n', EofCh)) ~ '\'' ^^ {
					case '\'' ~ chars ~ '\'' => StringLit(chars mkString "")
				}
					| '\"' ~ rep(chrExcept('\"', '\n', EofCh)) ~ '\"' ^^ {
					case '\"' ~ chars ~ '\"' => StringLit(chars mkString "")
				}
					| EofCh ^^^ EOF
					| '\'' ~> failure("unclosed string literal")
					| '\"' ~> failure("unclosed string literal")
					| delim
					| failure("illegal character")
					)

			override def identChar = letter | elem('_') | elem('.')
		}

		def statement:Parser[Statement] = (created | set | route | say) ~ opt(by) ~ opt(at) ^^ {
			case statement ~ by ~ at => {
				statement.by = by
				statement.at = at
				statement
			}
		}

		def by:Parser[String] = "by" ~> ident ^^ {
			case by => by
		}

		def at:Parser[String] = "@" ~> stringLit ^^ {
			case at => at
		}

		def created:Parser[Created] = "created" ^^ {
			case user => Created()
		}

		def set:Parser[Set] = "set" ~> ident ~ opt(stringLit) ^^ {
			case key ~ value => Set(key, value)
		}

		def route:Parser[Route] = "route" ~> "to" ~> ident ~ rep("then" ~> ident) ^^ {
			case user ~ next => Route(user, next)
		}

		def say:Parser[Say] = "say" ~> opt(stringLit) ^^ {
			case message => Say(message)
		}
	}

	private val statements: ListBuffer[Statement] = {
		val result = new ListBuffer[Statement]
		for (line <- read(file).map(trim) if line.length > 0 && ! startsWith(line, "#")) {
			val statement = parser.parseStatement(line)
			result += statement
		}
		result
	}
}

//object ScriptParser {
//	def parseScript(script:Seq[String]):Seq[Statement] = {
//		val result = new ListBuffer[Statement]
//		var i = 0
//		while (i < script.length) {
//			val line = script(i)
//			var statement = ScriptStatementParser.parseStatement(line)
//			statement match {
//				case value:Value if value.inlineValue.isEmpty => {
//
//					val buf = new StringBuilder
//					while (i + 1 < script.length && startsWith(script(i + 1), "  ")) {
//						i = i + 1
//						buf.append(script(i))
//						buf.append("\n")
//					}
//					value.multilineValue = Some(buf.toString)
//				}
//				case _ =>
//			}
//			i = i + 1
//			println("parsed statement: " + statement)
//			result += statement
//		}
//		result
//	}
//
//	def append(file:File, statement:Statement) {
//		writeStringToFile(file, "\nat \"%s\" by %s %s".format(statement.at.get, statement.by.get, statement.dsl), true)
//	}
//}


trait Statement {
	var by:Option[String] = None
	var at:Option[String] = None
	abstract override def toString:String = super.toString + by.map(" by " + _) + at.map(" @ \"" + _ + "\"")
}

trait Value {
	def value = inlineValue.getOrElse(multilineValue.getOrElse("??"))

	val inlineValue:Option[String]
	var multilineValue:Option[String] = None
}

case class Set(key:String, inlineValue:Option[String]) extends Statement with Value {
	override def toString:String = value.indexOf("\n") match {
		case -1 => "set %s \"%s\"".format(key, value)
		case _ => "set %s\n".format(key) + value.split("\n").mkString("  ", "\n  ", "\n")
	}
}

case class Created() extends Statement {
	override def toString:String = "created"
}

case class Route(user:String, next:Seq[String]) extends Statement {
	override def toString:String = null
}

case class Say(inlineValue:Option[String]) extends Statement with Value {
	override def toString:String = value.indexOf("\n") match {
		case -1 => "say \"%s\"".format(value)
		case _ => "say\n" + value.split("\n").mkString("  ", "\n  ", "\n")
	}
}