package com.landonkuhn.proseeo.main

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import java.io.File
import org.apache.commons.io.FileUtils
import com.landonkuhn.proseeo.document.DocumentIo

object Proseeo {

  private lazy val conf = new File(new File(".proseeo"), ".proseeo.conf")

  def main(args: Array[String]) {
    parseCommandLine(args.mkString(" ")) match {
      case Error(message) => System.err.println(message)
      case Help() => help
      case Init() => init
      case Info() => info
    }
  }

  private def help {
    println("Proseeo help!")
  }

  private def init {
    println("Processo init!")
    FileUtils.forceMkdir(new File(".proseeo"))
    FileUtils.touch(conf)
  }

  private def info {
    println("Processo info!")
    println(DocumentIo.read(conf))
  }

  def parseCommandLine(args: String): Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new StandardTokenParsers {

    override val lexical = new StdLexical {
      reserved += ("help", "init", "info")
      delimiters ++= List()
    }

    def command: Parser[Command] = help | init | info

    def help: Parser[Help] = "help" ^^ {
      case _ => Help()
    }

    def init: Parser[Init] = "init" ^^ {
      case _ => Init()
    }

    def info: Parser[Info] = "info" ^^ {
      case _ => Info()
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init() extends Command
case class Info() extends Command