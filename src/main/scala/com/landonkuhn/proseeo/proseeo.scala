package com.landonkuhn.proseeo

import java.io.File
import org.apache.commons.io.FileUtils.{touch, getUserDirectory}
import org.apache.commons.lang3.StringUtils._

import Logging._
import Ansi._
import plan.{Enum, Text, Field, Need, Want, Gate, Plan}
import scriptmodel.Created
import Util._
import Files._
import java.util.Date
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.apache.commons.lang3.StringUtils
import org.apache.commons.io.FileUtils

object Proseeo {

	lazy val projectDir = new File(".")
	lazy val projectFile = new File(projectDir, "proseeo.conf")

	lazy val userConf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
	lazy val user = userConf.getOrElseUpdate("user.name", System.getenv("USER"))
	lazy val storyId = userConf.get("projects.%s.using".format(projectId))

	lazy val projectConf = {
	  if (!projectFile.isFile) die("I don't see a project here")
	  new Conf(projectFile)
	}
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

	lazy val storiesDir = new File(projectDir, "stories")
	lazy val storyDir = new File(storiesDir, storyId.getOrElse(die("I don't know what story we're using")))
	lazy val scriptFile = new File(storyDir, "script.proseeo")
	lazy val script = {
	  if (!storiesDir.isDirectory) die("Missing stories directory %s".format(storiesDir))
	  if (!storyDir.isDirectory) die("Missing story directory %s".format(storyDir))
	  if (!scriptFile.isFile) die("Missing script file %s".format(scriptFile))
	  new scriptmodel.Script(scriptFile)
	}
	lazy val scriptState = script.play

	lazy val planFile:File = new File(storyDir, "plan.proseeo")
	lazy val plan:Plan = new Plan(planFile)
	def projectPlanFile(name:String):File = new File(projectDir, "%s.plan.proseeo".format(name))

	def main(args:Array[String]) = try {

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
			case cli.CatScript() => println(read(scriptFile).mkString("\n"))
			case cli.CatPlan() => println(read(planFile).mkString("\n"))
			case cli.EditScript() => doEditScript
  			case cli.EditPlan(global) => doEditPlan(global)
		}

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format
		userConf.save

		ok("ok")
	} catch {
		case ex:Exception => error("Sorry, I had an accident"); ex.printStackTrace
	}

	def doHelp {
		i("Proseeo 0.01")
	}

	def doStatus {
		i("Users:\n" + users.values.mkString("\n").indent)
		i("Groups:\n" + groups.values.mkString("\n").indent)
	}

  def doInit(name:String) {
		if (projectFile.isFile) die("There is already a project here")
		touch(projectFile)
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		projectConf.save
	}

  def doStart(name:Option[String]) {
		val storyId = Util.id
		val storyDir = new File(storiesDir, storyId)
		val scriptFile = new File(storyDir, "script.proseeo")
	  touch(scriptFile)
		val planFile = new File(storyDir, "plan.proseeo")
		touch(planFile)
		val script = new scriptmodel.Script(scriptFile)
		script.append(scriptmodel.Created(user, now)).save
		doUse(Some(storyId))
		for (name <- name) doPlan(Some(name), false)
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
		val _ = plan // force lazy evaluation

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
		val kw = if (kvs.isEmpty) 0 else kvs.map(_._1.length).max
		i((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v)).mkString("\n"))

		if (state.says.isEmpty) {
			i("")
			i("no says yet")
		}
		for (say <- state.says) {
			i("")
			i("%s\n  by %s\n  %s".format(say.text.bold, byStr(say.by), say.at.when(now)))
		}

		if (plan.groups.isEmpty) {
			i("")
			i("no plan")
		}
		var future = false
		for (group <- plan.groups) {
			val active = ! group.collect({ case x:Need => x }).forall(_.test(state.document))
			val fw = if (group.isEmpty) 0 else group.map(_.key.size).max
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

		if (!state.document.isEmpty) {
			i("")
			val kvs = (for (k <- state.document.keys.toSeq.sorted if plan.fields.isEmpty || ! plan.fields.contains(k)) yield k -> state.document(k))
			val kw = if (kvs.isEmpty) 0 else kvs.map(_._1.length).max
			i((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v.bold)).mkString("\n").indent)
		}
	}

	def doSay(message:String) {
		script.append(scriptmodel.Say(message, user, now)).save
	}

	def doSet(key:String, value:String, force:Boolean) {
		if (!force && !plan.fields.isEmpty && !plan.fields.contains(key)) die("Field [%s] is not in the plan".format(key))
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
				val source = projectPlanFile(name)
				if (!source.isFile) {
					if (!force) die("I don't have plan file %s".format(source))
					else FileUtils.touch(source)
				}
				FileUtils.copyFile(source, planFile)
				script.append(scriptmodel.Set("proseeo.plan", name, user, now)).save
			}
			case None => {
				if (planFile.isFile) planFile.delete
				FileUtils.touch(planFile)
				script.append(scriptmodel.Delete("proseeo.plan", user, now)).save
			}
		}
		doTell
	}

	def doCmd(args:Array[String]) {
		scala.sys.process.Process(args, storyDir) ! // later, shell-fu to make this better?
	}
	
	def doEditScript {
	  editor(scriptFile)
	}
	
	def doEditPlan(global:Boolean) {
		editor(plan.file)
	}
}