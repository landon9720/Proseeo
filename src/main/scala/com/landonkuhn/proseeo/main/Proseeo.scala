package com.landonkuhn.proseeo.main

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers

object Proseeo {
  def main(args: Array[String]) {
    parseCommandLine(args.mkString(" ")) match {
      case Error(message) => System.err.println(message)
      case Help() => help
      case Init() => init
    }
  }

  private def help {
    println("Proseeo help!")
  }

  private def init {
    println("Processo init!")
  }

  def parseCommandLine(args: String): Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new StandardTokenParsers {

    override val lexical = new StdLexical {
      reserved += ("help", "init")
      delimiters ++= List()
    }

    def command: Parser[Command] = help | init

    def help: Parser[Help] = "help" ^^ {
      case _ => Help()
    }

    def init: Parser[Init] = "init" ^^ {
      case _ => Init()
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init() extends Command