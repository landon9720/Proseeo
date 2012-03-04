package com.landonkuhn.proseeo

import play.Play
import scala.util.parsing.combinator.lexical.StdLexical
import scala.util.parsing.combinator.syntactical.StandardTokenParsers
import collection.JavaConversions._
import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import FileUtils._
import Implicits._

import Logging._
import Ansi.to_ansi_string
import CommandLineParser.parseCommandLine

object Proseeo {

  val userConf = {
    val f = new File(getUserDirectory, ".proseeo.conf")
    val c = new Conf(f)
    debug("User configuration [%s]:\n%s".format(f, c.toString.indent("  ")))
    c
  }

  val user = userConf.required("user")

  val projectConf = {
    val f = new File(new File("."), ".proseeo.conf")
    val c = new Conf(f)
    debug("Project configuration [%s]:\n%s".format(f, c.toString.indent("  ")))
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
}