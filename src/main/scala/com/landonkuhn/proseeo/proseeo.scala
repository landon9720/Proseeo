package com.landonkuhn.proseeo

import java.io.File
import org.apache.commons.io.FileUtils._
import org.apache.commons.lang3.StringUtils._

import Logging._
import Ansi._
import plan.{Enum, Text, Field, Need, Want, Gate, Plan}
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
	lazy val storyId = userConf.get("projects.%s.using".format(projectId))

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
		val f = new File(storiesDir, storyId.getOrElse("I don't know what story we're using"))
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

		val human = trim(args.mkString(" "))
		val willOfTheHuman = if (human == "") {
			if (storyId.isDefined) cli.Tell()
			else cli.Status() // later reports
		} else cli.CommandLineParser.parseCommandLine(human)
		willOfTheHuman match {
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
			case cli.Cmd(_) => doCmd(args.drop(1))
		}

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format
		userConf.save

		ok("ok")
	}

	def doHelp {
		i("Proseeo 0.01")
	}

	def doStatus {
		i("Users:\n" + users.values.mkString("\n").indent)
		i("Groups:\n" + groups.values.mkString("\n").indent)
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
		doUse(Some(storyId))
		for (name <- name) doPlan(Some(name), false) // later this nested call may fail after the start succeeds
	}

	def doEnd {
		script.append(scriptmodel.Ended(user, now)).save
	}

	def doUse(storyId:Option[String]) {
		storyId match {
			case Some(storyId) => {
				userConf += "projects.%s.using".format(projectId) -> storyId // later check it
				userConf += "projects.%s.name".format(projectId) -> projectName // nice touch
			}
			case None => {
				userConf -= "projects.%s.using".format(projectId)
				userConf -= "projects.%s.name".format(projectId)
			}
		}
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
		i((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v)).mkString("\n"))


		if (state.says.isEmpty) {
			i("")
			i("no says yet")
		}
		for (say <- state.says) {
			i("")
			i("%s\n  by %s\n  %s".format(say.text.bold, byStr(say.by), say.at.when(now)))
		}

		if (state.plan.isEmpty) {
			i("")
			i("no plan")
		}
		var future = false
		for (plan <- plan; group <- plan.groups) {
			val active = ! group.collect({ case x:Need => x }).forall(_.test(state.document))
			val fw = group.map(_.key.size).max
			i("")
			for (field <- group) {
				val prefix = field match {
					case w@Want(key, kind) if !w.test(state.document) => "want"
					case n@Need(Want(key, kind)) if !n.test(state.document) => "need"
					case _ => "    "
				}
				val value = field.kind match {
					case g:Gate if field.test(state.document) => Some("[*]")
					case _ => state.document.get(field.key)
				}
				val hint = field match {
					case field:Field if field.test(state.document) => ""
					case field:Field => field.kind match {
						case _:Text => "____"
						case Enum(values) => values.take(4).mkString(", ") + (values.drop(4).size match {
							case 0 => ""
							case n => ", %d more".format(n)
						})
						case _:Gate => "[ ]"
					}
				}
				val cursor = if (active && !future && !field.test(state.document)) ">" else " "
				i("%s %s %s  %s%s".format(cursor.bold, prefix, rightPad(field.key, fw), value.map(_ + " ").getOrElse("").bold, hint.yellow))
			}
			if (active) future = true
		}


		i("")
		i("document:\n" + state.document.toString.indent)
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

	def doPlan(name:Option[String], force:Boolean) {
		name match {
			case Some(name) => {
				if (loadPlan(name).isEmpty && !force) die("I don't have plan [%s]".format(name))
				script.append(scriptmodel.Plan(name, user, now)).save
			}
			case None => {
				script.append(scriptmodel.Unplan(user, now)).save
			}
		}
		doTell
	}

	def doCmd(args:Array[String]) {
		scala.sys.process.Process(args, storyDir) ! // later, shell-fu to make this better?
	}
}