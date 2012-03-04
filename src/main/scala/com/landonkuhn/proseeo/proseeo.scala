package com.landonkuhn.proseeo

import document.{Document, DocumentIo}
import play.Play
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import collection.JavaConversions._
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import FileUtils._
import Implicits._

import Logging._
import Ansi.to_ansi_string

object Proseeo {

  val userConf = {
    val f = new File(getUserDirectory, ".proseeo.conf")
    val c = new Conf(f)
    debug("User configuration [%s]:\n%s".format(f, c.toString.tab("  ")))
    c
  }

  val user = userConf.required("user")

  val projectConf = {
    val f = new File(new File("."), ".proseeo.conf")
    val c = new Conf(f)
    debug("Project configuration [%s]:\n%s".format(f, c.toString.tab("  ")))
    c
  }

  def main(args: Array[String]) {
    info("Proseeo v0.1".cyan)

    userConf += "count" -> (userConf.get("count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
    userConf.save

    parseCommandLine(args.mkString(" ")) match {
      case Error(message) => System.err.println(message)
      case Help() => help
      case Init(name) => init(name)
      case Info() => doinfo
      case Start() => start
      case Tell() => tell
      case Say(message) => say(message.getOrElse(Util.editor.get))
      case Set(key, value) => set(key, value.getOrElse(Util.editor.get))
    }
  }

  private def help {
    println("Proseeo help!")
  }

  private def init(name:String) {
    println("Proseeo init!")
    if (projectConf.file.exists) die("Project configuration already exists")
    projectConf += "name" -> name
    projectConf += "id" -> Util.id
    projectConf.save
  }

  private def doinfo {
    println("Proseeo info!")
//    println(DocumentIo.read(conf))
  }

  private def start {
    println("Proseeo start!")
    val uuid = Util.id
    val stories = new File("stories")
    val story = new File(stories, "%s.proseeo".format(uuid))
    val scriptFile = new File(story, "script")
    val created = script.Created()
    created.at = Some(Util.formatDateTime(new Date))
    created.by = Some(user)
    script.ScriptParser.append(scriptFile, created)
  }

  private def tell {
    val file = new File("script")
    if (!file.isFile) sys.error("not file: %s".format(file))
    val scriptObj = script.ScriptParser.parseScript(readLines(file).toSeq)
    println("script: " + scriptObj.mkString("\n"))
    val state = Play.play(scriptObj)
    println("document: " + state.document)
    println("stack: " + state.stack)
    println("current: " + state.current)
    println(state.comments.mkString("comments:\n\t", "\n\t", ""))
  }

  private def say(message: String) {
    val file = new File("script")
    if (!file.isFile) sys.error("not file: %s".format(file))
    val say = script.Say(Some(message))
    say.at = Some(Util.formatDateTime(new Date))
    say.by = Some(user)
    script.ScriptParser.append(file, say)
  }

  private def set(key: String, value: String) {
    val file = new File("script")
    if (!file.isFile) sys.error("not file: %s".format(file))
    val set = script.Set(key, Some(value))
    set.at = Some(Util.formatDateTime(new Date))
    set.by = Some(user)
    script.ScriptParser.append(file, set)
  }

  def parseCommandLine(args: String): Command = parser.phrase(parser.command)(new parser.lexical.Scanner(args)) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new StandardTokenParsers {

    override val lexical = new StdLexical {

      import scala.util.parsing.input.CharArrayReader.EofCh

      reserved += ("help", "init", "info", "start", "tell", "say", "set")
      delimiters ++= List()

      override def token: Parser[Token] =
        ( identChar ~ rep( identChar | digit )              ^^ { case first ~ rest => processIdent(first :: rest mkString "") }
        | digit ~ rep( digit )                              ^^ { case first ~ rest => NumericLit(first :: rest mkString "") }
        | '\'' ~ rep( chrExcept('\'', '\n', EofCh) ) ~ '\'' ^^ { case '\'' ~ chars ~ '\'' => StringLit(chars mkString "") }
        | '\"' ~ rep( chrExcept('\"', '\n', EofCh) ) ~ '\"' ^^ { case '\"' ~ chars ~ '\"' => StringLit(chars mkString "") }
        | EofCh                                             ^^^ EOF
        | '\'' ~> failure("unclosed string literal")
        | '\"' ~> failure("unclosed string literal")
        | delim
        | failure("illegal character")
      )

      override def identChar = letter | elem('_') | elem('.')
    }

    def command: Parser[Command] = help | init | info | start | tell | say | set

    def help: Parser[Help] = "help" ^^ {
      case _ => Help()
    }

    def init: Parser[Init] = "init" ~> ident ^^ {
      case name => Init(name)
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

    def say: Parser[Say] = "say" ~> opt(stringLit) ^^ {
      case message => Say(message)
    }

    def set: Parser[Set] = "set" ~> ident ~ opt(stringLit) ^^ {
      case key ~ value => Set(key, value)
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init(name:String) extends Command
case class Info() extends Command
case class Start() extends Command
case class Tell() extends Command
case class Say(message: Option[String]) extends Command
case class Set(key: String, value: Option[String]) extends Command

object Util {
  def formatDateTime(date: Date): String = ISODateTimeFormat.dateTime.print(new DateTime(date, DateTimeZone.UTC))
  def parseDateTime(s: String): Date = (new DateTime(s)).toDate

  def editor(): Option[String] = {
    val f = File.createTempFile("proceeo_", ".value")
    f.deleteOnExit

//    val out = new BufferedWriter(new FileWriter(f))
//    out.write(s)
//    out.close
//
//    debug("Launching editor: %s".format(f.getAbsolutePath))

    val args = System.getenv("EDITOR").split(" ").toList :+ f.getAbsolutePath
    val pb = new ProcessBuilder(args.toArray: _*)
    pb.start.waitFor

    Some(FileUtils.readLines(f).mkString("\n"))

//    val in = Source.fromFile(f)
//    val result = in.getLines.mkString("\n")
//    in.close
//    result
  }

  def id: String = java.util.UUID.randomUUID.toString.replace("-", "")

  def tab(s: String, tab: String): String = s.split("\n").map(tab + _).mkString("\n")
}



