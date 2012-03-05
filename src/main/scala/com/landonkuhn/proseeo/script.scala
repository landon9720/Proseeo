package com.landonkuhn.proseeo

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical
import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import Logging._
import Files._
import com.landonkuhn.proseeo.CommandLineParser.Say

class Script(file:File) {

	def append(statement:Statement):Script = {
		statements += statement
		this
	}

	def save:Script = {
		write(file, statements.map(_.toString))
		this
	}

	def play:State = {
		val document = new Document
		val history = new ListBuffer[String]
		var current:Option[Route] = None
		val comments = new ListBuffer[String]

		for (statement <- statements) statement match {
			case c@Created() => {

			}
			case set@Set(key, _) => document += key -> set.value
			case route@Route(user, next) => {
				history += user
				current = Some(route)
			}
			case say:Say => comments += say.value
			case x => die("I don't know about: " + x)
		}

		State(document, history, current, comments)
	}

	case class State(document:Document, stack:Seq[String], current:Option[Route], comments:Seq[String])

	trait Statement {
		var by:Option[String] = None
		var at:Option[String] = None
		protected def byat:String = "" + by.map(" by " + _).getOrElse("") + at.map(" at \"" + _ + "\"").getOrElse("")
	}

	case class Created() extends Statement {
		override def toString:String = "created" + byat
	}

	case class Set(key:String, value:String) extends Statement {
		override def toString:String = "set %s \"%s\"".format(key, value) + byat
	}

	case class Route(user:String, next:Seq[String]) extends Statement {
		override def toString:String = "route to %s" + next.map(" then " + _).mkString(" ")
	}

	case class Say(value:String) extends Statement {
		override def toString:String = "say \"%s\"".format(value)
	}

	private val parser = new StandardTokenParsers {

		def parseStatement(line:String):Statement = phrase(statement)(new lexical.Scanner(line)) match {
			case Success(statement, _) => statement
			case e:NoSuccess => die("I don't understand statement [%s] in file [%s]".format(line, file), e.msg)
		}

		override val lexical = new StdLexical {
			reserved +=(
				"by",
				"at",
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

		def statement = (created | set | route | say) ~ opt(by) ~ opt(at) ^^ {
			case statement ~ by ~ at => {
				statement.by = by
				statement.at = at
				statement
			}
		}

		def by = "by" ~> ident ^^ {
			case by => by
		}

		def at = "at" ~> stringLit ^^ {
			case at => at
		}

		def created = "created" ^^ {
			case user => Created()
		}

		def set = "set" ~> ident ~ stringLit ^^ {
			case key ~ value => Set(key, value)
		}

		def route = "route" ~> "to" ~> ident ~ rep("then" ~> ident) ^^ {
			case user ~ next => Route(user, next)
		}

		def say = "say" ~> stringLit ^^ {
			case message => Say(message)
		}
	}

	private val statements:ListBuffer[Statement] = {
		val result = new ListBuffer[Statement]
		result ++= (for (line <- read(file).map(trim) if line.length > 0 && !startsWith(line, "#")) yield {
			parser.parseStatement(line)
		})
		result
	}
}