package com.landonkuhn.proseeo.main

import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import java.io.File
import org.apache.commons.io.FileUtils._
import com.landonkuhn.proseeo.document.DocumentIo
import java.util.UUID
import collection.JavaConversions._
import org.apache.commons.lang3.StringUtils._

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
      case Tell() => tell
      case Say(message) => say(message)
    }
  }

  private def help {
    println("Proseeo help!")
  }

  private def init {
    println("Proseeo init!")
//    forceMkdir(new File(".proseeo"))
    //touch(conf)
    writeStringToFile(conf, """name: new-project
uuid: %s
""".format(UUID.randomUUID.toString))
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

  private def tell {
    val file = new File("script")
    if (!file.isFile) sys.error("not file: %s".format(file))
    val script = ScriptParser.parseScript(readLines(file).toSeq)
    println("script: " + script.mkString("\n"))
    val state = Play.play(script)
    println("document: " + state.document)
    println("stack: " + state.stack)
    println("current: " + state.current)
    println(state.comments.mkString("comments:\n\t", "\n\t", ""))
//    println(state.route)

  }

  private def say(message: String) {
    val file = new File("script")
    if (!file.isFile) sys.error("not file: %s".format(file))
    ScriptParser.append(file, SayStatement(Some(message)))
  }

  def parseCommandLine(args: String): Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new StandardTokenParsers {

    override val lexical = new StdLexical {
      reserved += ("help", "init", "info", "start", "tell", "say")
      delimiters ++= List()
    }

    def command: Parser[Command] = help | init | info | start | tell | say

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

    def tell: Parser[Tell] = "tell" ^^ {
      case _ => Tell()
    }

    def say: Parser[Say] = "say" ~> stringLit ^^ {
      case message => Say(message)
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init() extends Command
case class Info() extends Command
case class Start() extends Command
case class Tell() extends Command
case class Say(message: String) extends Command