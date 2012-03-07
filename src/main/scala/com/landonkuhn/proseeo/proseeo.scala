package com.landonkuhn.proseeo

import java.io.File
import org.apache.commons.io.FileUtils._
import org.apache.commons.lang3.StringUtils._

import Logging._
import Ansi._
import plan.Plan
import scriptmodel.Created
import Util._
import java.util.Date
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.apache.commons.lang3.StringUtils

object Proseeo {

	lazy val projectDir = new File(".")
	lazy val projectFile = new File(projectDir, ".proseeo.conf") // later no leading .

	lazy val userConf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
	lazy val user = userConf.required("user.name")
	lazy val storyId = userConf.required("projects.%s.using".format(projectId))

	lazy val projectConf = new Conf(projectFile)
	lazy val projectName = projectConf.required("project.name")
	lazy val projectId = projectConf.required("project.id")

	case class User(userName:String, fullName:Option[String], email:Option[String])
	lazy val users = (for ((userName, user) <- new Document(projectConf).scope("project.users.").tree.subtrees) yield {
		userName -> User(userName, user.leaf("name"), user.leaf("email"))
	}).toMap

	case class Group(groupName:String, members:Set[User])
	lazy val groups = (for ((group, members) <- new Document(projectConf).scope("project.groups.").tree.leafs) yield {
		group -> Group(group, members.split(",").map(trim).map(users(_)).toSet)
	}).toMap

	lazy val storiesDir = {
		val f = new File(projectDir, "stories")
		if (!f.isDirectory) die("Missing stories directory %s".format(f))
		f
	}
	lazy val storyDir = {
		val f = new File(storiesDir, storyId)
		if (!f.isDirectory) die("Missing story directory %s".format(f))
		f
	}
	lazy val scriptFile = {
		val f = new File(storyDir, "script")
		if (!f.isFile) die("Missing script file %s".format(f))
		f
	}
	lazy val script = new scriptmodel.Script(scriptFile)

	lazy val plan:Option[Plan] = {
		val name:Option[String] = script.play.plan
		val plan:Option[Plan] = name.flatMap(loadPlan(_).orElse({ warn("I don't know plan [%s]".format(name.get)); None }))
		plan
	}
	private def loadPlan(name:String):Option[Plan] = Seq(storyDir, projectDir)
		.map(dir => new File(dir, "%s.plan.proseeo".format(name)))
		.find(_.isFile)
		.map(new Plan(name, _))

	def main(args:Array[String]) {

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format

		cli.CommandLineParser.parseCommandLine(args.mkString(" ")) match {
			case cli.Help() => doHelp
			case cli.Status() => doStatus
			case cli.Init(name) => doInit(name)
			case cli.Start(name) => doStart(name)
			case cli.End() => doEnd
			case cli.Use(storyId) => doUse(storyId)
			case cli.Tell() => doTell
			case cli.Say(message) => doSay(message)
			case cli.Set(key, value, force) => doSet(key, value, force)
			case cli.Delete(key) => doDelete(key)
			case cli.RouteTo(name, then) => doRouteTo(name, then)
			case cli.Plan(name, force) => doPlan(name, force)
		}

		userConf.save
		ok("ok")
	}

	def doHelp {
		info("Proseeo 0.01")
	}

	def doStatus {
		info("Users:\n" + users.values.mkString("\n").indent)
		info("Groups:\n" + groups.values.mkString("\n").indent)
	}

  def doInit(name:String) {
		if (projectConf.file.exists) die("There is already a project here")
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		projectConf.save
	}

  def doStart(name:Option[String]) {
		val storyId = Util.id
		val story = new File(new File("stories"), storyId)
		val scriptFile = new File(story, "script")
		touch(scriptFile)
		val script = new scriptmodel.Script(scriptFile)
		script.append(scriptmodel.Created(user, now)).save
		doUse(storyId)
		for (name <- name) doPlan(name, false) // later this nested call may fail after the start succeeds
	}

	def doEnd {
		script.append(scriptmodel.Ended(user, now))save
	}

	def doUse(storyId:String) {
		userConf += "projects.%s.using".format(projectId) -> storyId // later check it
		userConf += "projects.%s.name".format(projectId) -> projectName // nice touch
	}

	def doTell {
		import StringUtils._

		val state = script.play

		def atStr(date:Date) = "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date)).bold, date.when(now))

		def byStr(name:String) = users.get(name) match {
			case Some(User(_, fullName, email)) =>
				"%s<%s>%s".format(fullName.map(_ + " ").getOrElse("").bold, name, email.map(" " + ).getOrElse(""))
			case None => "<%s>".format(name)
		}

		def atbyStr(date:Date, name:String) = "%s by %s".format(atStr(date), byStr(name))

		val kvs = (state.created match {
			case Some(scriptmodel.Created(by, at)) => List(("created", atbyStr(at, by)))
			case None => Nil
		}) ::: (state.ended match {
			case Some(scriptmodel.Ended(by, at)) => List(("closed", atbyStr(at, by)))
			case None => Nil
		}) ::: (state.where match {
			case Some(a:Actor) => List(("where", a.toString.bold)) // later
			case None => Nil
		}) ::: Nil
		val kw = kvs.map(_._1.length).max
		info((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v)).mkString("\n"))

		info("")
		info(if (state.says.isEmpty) "no comments yet" else "%d comments".format(state.says.size))

		for (say <- state.says) {
			info("")
			info("%s\n  by %s\n  %s".format(say.text.bold, byStr(say.by), say.at.when(now)))
		}


		info("")
		info("document:\n" + state.document.toString.indent)


		info("plan name: " + state.plan)
		info("plan:\n")
		plan match {
			case None => println("  (none)")
			case Some(plan) => plan(state)
		}
	}

	def doSay(message:String) {
		script.append(scriptmodel.Say(message, user, now)).save
	}

	def doSet(key:String, value:String, force:Boolean) {
		for (plan <- plan) if (!force && !plan.fields.contains(key)) die("Field [%s] is not in the plan [%s]".format(key, plan.name))
		script.append(scriptmodel.Set(key, value, user, now)).save
	}

	def doDelete(key:String) {
		script.append(scriptmodel.Delete(key, user, now)).save
	}

	def doRouteTo(name:Actor, then:Seq[Actor]) {
		script.append(scriptmodel.RouteTo(name, then, user, now)).save
	}

	def doPlan(name:String, force:Boolean) {
		if (loadPlan(name).isEmpty && !force) die("I don't have plan [%s]".format(name))
		script.append(scriptmodel.Plan(name, user, now)).save
	}
}