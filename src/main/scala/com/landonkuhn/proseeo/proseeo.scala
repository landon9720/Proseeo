package com.landonkuhn.proseeo

import play.Play
import collection.JavaConversions._
import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import FileUtils._
import Implicits._
import Util._

import Logging._
import Ansi.to_ansi_string
import CommandLineParser.parseCommandLine

object Proseeo {

  lazy val userConf = {
    val f = new File(getUserDirectory, ".proseeo.conf")
    val c = new Conf(f)
    debug("User configuration [%s]:\n%s".format(f, c.toString.indent("  ")))
    c
  }
  lazy val user = userConf.required("user.name")
  lazy val storyId = userConf.required("projects.%s.using".format(projectId))

  lazy val projectConf = {
    val f = new File(new File("."), ".proseeo.conf")
    val c = new Conf(f)
    debug("Project configuration [%s]:\n%s".format(f, c.toString.indent("  ")))
    c
  }
  lazy val projectName = projectConf.required("project.name")
  lazy val projectId = projectConf.required("project.id")

	lazy val projectDir = new File(".")
	lazy val storiesDir = {
		val f = new File(projectDir, "stories")
		if (! f.isDirectory) die("Missing stories directory [%s]".format(f))
		f
	}
	lazy val storyDir = {
		val f = new File(storiesDir, storyId)
		if (! f.isDirectory) die("Missing story directory [%s]".format(f))
		f
	}
	lazy val scriptFile = {
		val f = new File(storyDir, "script")
		if (! f.isFile) die("Missing script file [%s]".format(f))
		f
	}

  def main(args: Array[String]) {
    info("Proseeo v0.1".cyan)

    userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
    userConf += "stats.last" -> formatDateTime(now)

    parseCommandLine(args.mkString(" ")) match {
      case Error(message) => die(message)
      case Help() => dohelp
      case Init(name) => doinit(name)
      case Info() => doinfo
      case Use(storyId) => douse(storyId)
      case Start() => start
      case Tell() => tell
      case Say(message) => say(message.getOrElse(Util.editor.get))
      case Set(key, value) => set(key, value.getOrElse(Util.editor.get))
    }

	  userConf.save
  }

  private def dohelp {
    println("Proseeo dohelp!")
  }

  private def doinit(name:String) {
    println("Proseeo doinit!")
    if (projectConf.file.exists) die("Project configuration already exists")
    projectConf += "project.name" -> name
    projectConf += "project.id" -> Util.id
    debug("Writing project [%s] configuration:\n%s".format(name, projectConf.toString.indent("  ")))
    projectConf.save
  }

  private def doinfo {
    println("Proseeo info!")
//    println(DocumentIo.read(conf))
  }

  private def douse(storyId:String) {
    info("Proseeo use %s".format(storyId))
    userConf += "projects.%s.using".format(projectId) -> storyId // TODO check it
  }

  private def start {
    info("Proseeo start")
    val storyId = Util.id
    val story = new File(new File("stories"), storyId)
    val scriptFile = new File(story, "script")
    val created = script.Created()
    created.at = Some(formatDateTime(new Date))
    created.by = Some(user)
    script.ScriptParser.append(scriptFile, created)
    douse(storyId)
  }

  private def tell {
    val scriptObj = script.ScriptParser.parseScript(readLines(scriptFile).toSeq)
    info("script: " + scriptObj.mkString("\n"))
    val state = Play.play(scriptObj)
    info("document: " + state.document)
    info("stack: " + state.stack)
    info("current: " + state.current)
    info(state.comments.mkString("comments:\n\t", "\n\t", ""))
  }

  private def say(message: String) {
    val say = script.Say(Some(message))
    say.at = Some(Util.formatDateTime(new Date))
    say.by = Some(user)
    script.ScriptParser.append(scriptFile, say)
  }

  private def set(key: String, value: String) {
    val set = script.Set(key, Some(value))
    set.at = Some(Util.formatDateTime(new Date))
    set.by = Some(user)
    script.ScriptParser.append(scriptFile, set)
  }
}