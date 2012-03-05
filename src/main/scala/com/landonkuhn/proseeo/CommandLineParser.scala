package com.landonkuhn.proseeo

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical

object CommandLineParser {

	def parseCommandLine(args:String):Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
		case parser.Success(command, _) => command
		case e:parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
	}

	private val parser = new StandardTokenParsers {

		override val lexical = new StdLexical {

			import scala.util.parsing.input.CharArrayReader.EofCh

			reserved +=(
				"help", "init", "info", "use",
				"start", "tell", "say", "set"
				)
			delimiters ++= List()

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

		def command:Parser[Command] = help | init | info | use | start | tell | say | set

		def help:Parser[Help] = "help" ^^ {
			case _ => Help()
		}

		def init:Parser[Init] = "init" ~> ident ^^ {
			case name => Init(name)
		}

		def info:Parser[Info] = "info" ^^ {
			case _ => Info()
		}

		def use:Parser[Use] = "use" ~> ident ^^ {
			case storyId => Use(storyId)
		}

		def start:Parser[Start] = "start" ^^ {
			case _ => Start()
		}

		def tell:Parser[Tell] = "tell" ^^ {
			case _ => Tell()
		}

		def say:Parser[Say] = "say" ~> success ^^ {
			case message => Say(message)
		}

		def set = "set" ~> ident ~ success ^^ {
			case key ~ value => Set(key, value)
		}
	}

	trait Command

	case class Error(message:String) extends Command

	case class Help() extends Command

	case class Init(name:String) extends Command

	case class Use(storyId:String) extends Command

	case class Info() extends Command

	case class Start() extends Command

	case class Tell() extends Command

	case class Say(message:String) extends Command

	case class Set(key:String, value:String) extends Command

}