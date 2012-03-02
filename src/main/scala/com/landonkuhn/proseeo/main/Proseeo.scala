package com.landonkuhn.proseeo.main

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import java.io.File
import org.apache.commons.io.FileUtils._
import com.landonkuhn.proseeo.document.DocumentIo
import java.util.UUID
import collection.JavaConversions._
import org.apache.commons.lang.StringUtils._

object Proseeo {

  private lazy val userConf = new File(new File(System.getProperty("user.home")), ".proseeo.conf")
  println("userConf: " + userConf)
  private lazy val userConfDocument = DocumentIo.read(userConf)
  println("userConfDocument:\n" + userConfDocument)

  private lazy val conf = new File(new File(".proseeo"), ".proseeo.conf")
  println("conf: " + conf)

  def main(args: Array[String]) {
    parseCommandLine(args.mkString(" ")) match {
      case Error(message) => System.err.println(message)
      case Help() => help
      case Init() => init
      case Info() => info
      case Start() => start
    }
  }

  private def help {
    println("Proseeo help!")
  }

  private def init {
    println("Proseeo init!")
//    forceMkdir(new File(".proseeo"))
    touch(conf)
  }

  private def info {
    println("Proseeo info!")
    println(DocumentIo.read(conf))
  }

  private def start {
    println("Proseeo start!")
    val uuid = UUID.randomUUID.toString
    val stories = new File("stories")
    val story = new File(stories, "%s.proseeo".format(uuid))
    val script = new File(story, "script")
    touch(script)
  }

  def parseCommandLine(args: String): Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new StandardTokenParsers {

    override val lexical = new StdLexical {
      reserved += ("help", "init", "info", "start")
      delimiters ++= List()
    }

    def command: Parser[Command] = help | init | info | start

    def help: Parser[Help] = "help" ^^ {
      case _ => Help()
    }

    def init: Parser[Init] = "init" ^^ {
      case _ => Init()
    }

    def info: Parser[Info] = "info" ^^ {
      case _ => Info()
    }

    def start: Parser[Start] = "start" ^^ {
      case _ => Start()
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init() extends Command
case class Info() extends Command
case class Start() extends Command