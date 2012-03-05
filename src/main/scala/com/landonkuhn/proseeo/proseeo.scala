package com.landonkuhn.proseeo

import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import FileUtils._
import Util._

import Logging._
import Ansi.to_ansi_string
import com.landonkuhn.proseeo.CommandLineParser.{Say, parseCommandLine}

object Proseeo {

	lazy val projectDir = new File(".")
	lazy val projectFile = new File(projectDir, ".proseeo.conf")

	lazy val userConf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
	lazy val user = userConf.required("user.name")
	lazy val storyId = userConf.required("projects.%s.using".format(projectId))

	lazy val projectConf = new Conf(projectFile)
	lazy val projectName = projectConf.required("project.name")
	lazy val projectId = projectConf.required("project.id")

	lazy val storiesDir = {
		val f = new File(projectDir, "stories")
		if (!f.isDirectory) die("Missing stories directory [%s]".format(f))
		f
	}
	lazy val storyDir = {
		val f = new File(storiesDir, storyId)
		if (!f.isDirectory) die("Missing story directory [%s]".format(f))
		f
	}
	lazy val scriptFile = {
		val f = new File(storyDir, "script")
		if (!f.isFile) die("Missing script file [%s]".format(f))
		f
	}
	lazy val script = new Script(scriptFile)

	def main(args:Array[String]) {
		info("Proseeo v0.1".cyan)

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format

		parseCommandLine(args.mkString(" ")) match {
			case CommandLineParser.Help() => doHelp
			case CommandLineParser.Status() => doStatus
      case CommandLineParser.Init(name) => doInit(name)
      case CommandLineParser.Start() => doStart
      case CommandLineParser.Use(storyId) => doUse(storyId)
			case CommandLineParser.Tell() => doTell
			case CommandLineParser.Say(message) => doSay(message)
			case CommandLineParser.Set(key, value) => doSet(key, value)
		}

		userConf.save
	}

	def doHelp {
		info("Proseeo help!")
	}

	def doStatus {
		info("Proseeo status!")
	}

  def doInit(name:String) {
		info("Proseeo doinit!")
		if (projectConf.file.exists) die("Project configuration already exists")
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		debug("Writing project [%s] configuration:\n%s".format(name, projectConf.toString.indent("  ")))
		projectConf.save
	}

  def doStart {
		info("Proseeo start")
		val storyId = Util.id
		val story = new File(new File("stories"), storyId)
		val scriptFile = new File(story, "script")
		touch(scriptFile)
		val script = new Script(scriptFile)
		script.append(ScriptStatementParser.Created(user, now)).save
		doUse(storyId)
	}

	def doUse(storyId:String) {
		info("Proseeo use %s".format(storyId))
		userConf += "projects.%s.using".format(projectId) -> storyId // TODO check it
	}

	def doTell {
		script.play
//		val state = script.play
//		info("document: " + state.document)
//		info("stack: " + state.stack)
//		info("current: " + state.current)
//		info(state.comments.mkString("comments:\n\t", "\n\t", ""))
	}

	def doSay(message:String) {
		script.append(ScriptStatementParser.Say(message, user, now)).save
	}

	def doSet(key:String, value:String) {
		script.append(ScriptStatementParser.Set(key, value, user, now)).save
	}
}