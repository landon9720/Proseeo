package com.landonkuhn.proseeo

import java.io.File
import org.apache.commons.io.FileUtils._
import org.apache.commons.lang3.StringUtils._

import Logging._
import Ansi._
import plan.Plan
import Util._

object Proseeo {

	lazy val projectDir = new File(".")
	lazy val projectFile = new File(projectDir, ".proseeo.conf") // later no leading .

	lazy val userConf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
	lazy val user = userConf.required("user.name")
	lazy val storyId = userConf.required("projects.%s.using".format(projectId))

	lazy val projectConf = new Conf(projectFile)
	lazy val projectName = projectConf.required("project.name")
	lazy val projectId = projectConf.required("project.id")

	case class User(userName:String, fullName:String, email:String)
	lazy val users = (for ((userName, user) <- new Document(projectConf).scope("project.users.").tree.subtrees) yield {
		userName -> User(userName, user.leaf("name").getOrElse("<unknown name>"), user.leaf("email").getOrElse("<unknown email>"))
	}).toMap

	case class Group(groupName:String, members:collection.Set[User])
	lazy val groups = (for ((group, members) <- new Document(projectConf).scope("project.groups.").tree.leafs) yield {
		group -> Group(group, members.split(",").map(trim).map(users(_)).toSet)
	}).toMap

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
	lazy val script = new scriptmodel.Script(scriptFile)

	lazy val plan:Option[Plan] = {
		script.play.plan.flatMap(name =>
			(Seq(storyDir, projectDir)
				.map(dir => new File(dir, "%s.plan".format(name)))
				.find(_.isFile)
				.map(new Plan(_))
			).orElse({ warn("I do not know plan [%s]".format(name)); None })
		)
	}

	def main(args:Array[String]) {
		info("Proseeo v0.1".cyan)

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format

		cli.CommandLineParser.parseCommandLine(args.mkString(" ")) match {
			case cli.Help() => doHelp
			case cli.Status() => doStatus
			case cli.Init(name) => doInit(name)
			case cli.Start() => doStart
			case cli.End() => doEnd
			case cli.Use(storyId) => doUse(storyId)
			case cli.Tell() => doTell
			case cli.Say(message) => doSay(message)
			case cli.Set(key, value) => doSet(key, value)
			case cli.RouteTo(name, then) => doRouteTo(name, then)
			case cli.Plan(name) => doPlan(name)
		}

		userConf.save
	}

	def doHelp {
		info("Proseeo help!")
	}

	def doStatus {
		info("Proseeo status!")
		info("Users:\n" + users.values.mkString("\n").indent)
		info("Groups:\n" + groups.values.mkString("\n").indent)
	}

  def doInit(name:String) {
		info("Proseeo doinit!")
		if (projectConf.file.exists) die("Project configuration already exists")
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		debug("Writing project [%s] configuration:\n%s".format(name, projectConf.toString.indent))
		projectConf.save
	}

  def doStart {
		info("Proseeo start")
		val storyId = Util.id
		val story = new File(new File("stories"), storyId)
		val scriptFile = new File(story, "script")
		touch(scriptFile)
		val script = new scriptmodel.Script(scriptFile)
		script.append(scriptmodel.Created(user, now)).save
		doUse(storyId)
	}

	def doEnd {
		info("Proseeo end")
		script.append(scriptmodel.Ended(user, now))save
	}

	def doUse(storyId:String) {
		info("Proseeo use %s".format(storyId))
		userConf += "projects.%s.using".format(projectId) -> storyId // later check it
		userConf += "projects.%s.name".format(projectId) -> projectName // nice touch
	}

	def doTell {
		val state = script.play
		info("created: " + state.created)
		info("ended: " + state.ended)
		info("says:\n" + state.says.mkString("\n").indent)
		info("document:\n" + state.document.toString.indent)
		info("where: " + state.where)
		info("plan name: " + state.plan)
		info("plan:\n" + (plan match {
			case None => "(none)"
			case Some(plan) => plan.toString
		}).indent)
	}

	def doSay(message:String) {
		script.append(scriptmodel.Say(message, user, now)).save
	}

	def doSet(key:String, value:String) {
		script.append(scriptmodel.Set(key, value, user, now)).save
	}

	def doRouteTo(name:Actor, then:Seq[Actor]) {
		script.append(scriptmodel.RouteTo(name, then, user, now)).save
	}

	def doPlan(name:String) {
		info("Planning [%s]".format(name))
		script.append(scriptmodel.Plan(name, user, now)).save
	}
}