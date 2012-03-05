package com.landonkuhn.proseeo

import collection.JavaConversions._
import java.util.Date
import java.io.File
import org.apache.commons.io.FileUtils
import FileUtils._
import Implicits._
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
		userConf += "stats.last" -> formatDateTime(now)

		parseCommandLine(args.mkString(" ")) match {
			case CommandLineParser.Error(message) => die(message)
			case CommandLineParser.Help() => doHelp
			case CommandLineParser.Init(name) => doInit(name)
			case CommandLineParser.Info() => doInfo
			case CommandLineParser.Use(storyId) => doUse(storyId)
			case CommandLineParser.Start() => doStart
			case CommandLineParser.Tell() => doTell
			case CommandLineParser.Say(message) => doSay(message.getOrElse(Util.editor.get))
			case CommandLineParser.Set(key, value) => doSet(key, value.getOrElse(Util.editor.get))
		}

		userConf.save
	}

	def doHelp {
		info("Proseeo help!")
	}

	def doInit(name:String) {
		info("Proseeo doinit!")
		if (projectConf.file.exists) die("Project configuration already exists")
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		debug("Writing project [%s] configuration:\n%s".format(name, projectConf.toString.indent("  ")))
		projectConf.save
	}

	def doInfo {
		info("Proseeo info!")
	}

	def doUse(storyId:String) {
		info("Proseeo use %s".format(storyId))
		userConf += "projects.%s.using".format(projectId) -> storyId // TODO check it
	}

	def doStart {
		info("Proseeo start")
		val storyId = Util.id
		val story = new File(new File("stories"), storyId)
		val scriptFile = new File(story, "script")
		touch(scriptFile)
		val script = new Script(scriptFile)
		val created = script.Created()
		created.at = Some(formatDateTime(new Date))
		created.by = Some(user)
		script.append(created).save
		doUse(storyId)
	}

	def doTell {
		val state = script.play
		info("document: " + state.document)
		info("stack: " + state.stack)
		info("current: " + state.current)
		info(state.comments.mkString("comments:\n\t", "\n\t", ""))
	}

	def doSay(message:String) {
		val say = script.Say(Some(message))
		say.at = Some(Util.formatDateTime(new Date))
		say.by = Some(user)
		script.append(say).save
	}

	def doSet(key:String, value:String) {
		val set = script.Set(key, Some(value))
		set.at = Some(Util.formatDateTime(new Date))
		set.by = Some(user)
		script.append(set).save
	}
}