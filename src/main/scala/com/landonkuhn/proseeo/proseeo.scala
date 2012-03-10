package com.landonkuhn.proseeo

import java.io.File


import Logging._
import Ansi._
import plan._
import Util._
import java.util.Date
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils._
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils._

object Proseeo {

	lazy val projectDir = {
		val dirs = Seq(new File("."), new File(".."))
		val conf = dirs.find(new File(_, "project.proseeo").isFile)
		conf.getOrElse(die("I don't see a project here. change to a project directory, or create one here using p init"))
	}
	lazy val projectFile = new File(projectDir, "project.proseeo")

	lazy val userConf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
	lazy val user = userConf.getOrElseUpdate("user.name", System.getenv("USER"))
	lazy val storyId:Option[String] = {
		def projectUsing = userConf.get("projects.%s.using".format(projectId))
		if (Option(new File(".").getCanonicalFile.getParentFile) == Some(projectDir.getCanonicalFile)) {
			val storyId = Some(new File(".").getCanonicalFile.getName)
			if (projectUsing != storyId) {
				doUse(storyId)
				warn("this is a story directory, so we are going to use the story here")
			}
		}
		projectUsing
	}

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

	lazy val storyDir = new File(projectDir, storyId.getOrElse(die("I don't know what story we're using")))
	lazy val scriptFile = new File(storyDir, "script.proseeo")
	lazy val script = {
	  if (!storyDir.isDirectory) die("missing story directory %s".format(storyDir))
	  if (!scriptFile.isFile) die("missing script file %s".format(scriptFile))
	  new scriptmodel.Script(scriptFile)
	}
	lazy val scriptState = script.play

	lazy val planFile:File = new File(storyDir, "plan.proseeo")
	lazy val plan:Plan = new Plan(planFile)
	def projectPlanFile(name:String):File = new File(projectDir, "%s.plan.proseeo".format(name))

	var say_ok = true
	def main(args:Array[String]) = try {

		val willOfTheHuman = if (args.isEmpty) {
			if (storyId.isDefined) cli.Tell()
			else cli.Status() // later reports
		} else cli.CommandLineParser.parseCommandLine(args.toList)
		willOfTheHuman match {
			case cli.Help() => doHelp
			case cli.Status() => doStatus
			case cli.Init(name) => doInit(name)
			case cli.Start(name) => doStart(name)
			case cli.End() => doEnd
			case cli.Use(name) => doUse(name)
			case cli.Tell() => doTell
			case cli.Say(message) => doSay(message)
			case cli.Set(key, value) => doSet(key, value)
			case cli.Delete(key) => doDelete(key)
			case cli.Route(actors) => doRoute(actors)
			case cli.Plan(name) => doPlan(name)
			case cli.Locate(name) => doLocate(name)
			case cli.Attach(files) => doAttach(files)
		}

		userConf += "stats.count" -> (userConf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		userConf += "stats.last" -> now.format
		userConf.save

		if (say_ok) ok("ok")
	} catch {
		case ex:Exception => error("sorry, I had an accident"); ex.printStackTrace
	}

	def doHelp {
		i("Proseeo 0.01")
	}

	def doStatus {
		i("Users:\n" + users.values.mkString("\n").indent)
		i("Groups:\n" + groups.values.mkString("\n").indent)
	}

  def doInit(name:String) {
		val projectFile = new File("project.proseeo")
	  if (projectFile.isFile) die("there is already a project here")
		touch(projectFile)
		projectConf += "project.name" -> name
		projectConf += "project.id" -> Util.id
		projectConf.save
	}

  def doStart(name:String) {
		val storyDir = (new File(projectDir, name) +: (for (i <- (1 to Int.MaxValue).view) yield new File(projectDir, "%s-%d".format(name, i)))).find(!_.exists).get
		val scriptFile = new File(storyDir, "script.proseeo")
	  touch(scriptFile)
		val planFile = new File(storyDir, "plan.proseeo")
		touch(planFile)
		val script = new scriptmodel.Script(scriptFile)
		script.append(scriptmodel.Created(user, now)).save
	  i("started story %s".format(storyDir.getName))
		doUse(Some(storyDir.getName))
	}

	def doEnd {
		script.append(scriptmodel.Ended(user, now)).save
	}

	def doUse(name:Option[String]) {
		name match {
			case Some(name) => {
				userConf += "projects.%s.using".format(projectId) -> name // later check it
				userConf += "projects.%s.name".format(projectId) -> projectName // nice touch
			}
			case None => {
				userConf -= "projects.%s.using".format(projectId)
				userConf -= "projects.%s.name".format(projectId)
			}
		}
		userConf.save
	}

	def doTell {
		import StringUtils._

		// force lazy evaluation
		val state = scriptState
		val _ = plan

		def atStr(date:Date) = "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date)).bold, date.when(now))

		def byStr(name:String) = users.get(name) match {
			case Some(User(_, fullName, email)) =>
				"%s<%s>%s".format(fullName.map(_ + " ").getOrElse("").bold, name, email.map(" " + ).getOrElse(""))
			case None => "<%s>".format(name)
		}

		def atbyStr(date:Date, name:String) = "%s by %s".format(atStr(date), byStr(name))

		val kvs =
			(List("story" -> storyDir.getName.bold)
			) ::: (state.document.get("proseeo.plan") match {
				case Some(plan) => List("plan" -> plan.bold)
				case None => List("plan" -> "no plan (use p plan name)".yellow)
			}) ::: (state.created match {
				case Some(scriptmodel.Created(by, at)) => List("created" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.ended match {
				case Some(scriptmodel.Ended(by, at)) => List("closed" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.where match {
				case Some(a:String) => List("where" -> a.toString.bold) // later
				case None => Nil
			}) ::: Nil
		val kw = if (kvs.isEmpty) 0 else kvs.map(_._1.length).max
		i((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v)).mkString("\n"))

		for (say <- state.says) {
			i("")
			i("%s\n  by %s\n  %s".format(say.text.bold, byStr(say.by), say.at.when(now)))
		}

		var future = false
		for (group <- plan.groups) {
			val active = ! group.collect({ case x:Need => x }).forall(_.test(state.document))
			val fw = if (group.isEmpty) 0 else group.map(_.key.size).max
			if (!group.isEmpty) i("")
			for (field <- group) {
				val prefix = field match {
					case w@Want(key, kind) if !w.test(state.document) => "want"
					case n@Need(Want(key, kind)) if !n.test(state.document) => "need"
					case _ => "    "
				}
				val value = field.kind match {
					case g:Gate if field.test(state.document) => Some("[*]")
					case ts:TimeStamp if field.test(state.document) => {
						val date = state.document(field.key).toDate
						Some("%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss").print(new DateTime(date)).bold, date.when(now)))
					}
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
						case _:TimeStamp => "timestamp"
					}
				}
				val cursor = if (active && !future && !field.test(state.document)) ">" else " "
				i("%s %s | %s  %s%s".format(cursor.bold, prefix, rightPad(field.key, fw), value.map(_ + " ").getOrElse("").bold, hint.yellow))
			}
			if (active) future = true
		}

		if (!state.document.isEmpty) {
			def value(s:String):String = {
				s.optDate match {
					case Some(date) => "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss").print(new DateTime(date)).bold, date.when(now))
					case None => s
				}
			}
			val kvs = (for (k <- state.document.keys.toSeq.sorted if !k.startsWith("proseeo.") && (plan.fields.isEmpty || ! plan.fields.contains(k))) yield k -> state.document(k))
			if (!kvs.isEmpty) {
				i("")
				val kw = if (kvs.isEmpty) 0 else kvs.map(_._1.length).max
				i((for ((k, v) <- kvs) yield "     | %s  %s".format(rightPad(k, kw), value(v))).mkString("\n").indent)
			}
		}
	}

	def doSay(message:String) {
		script.append(scriptmodel.Say(message, user, now)).save
	}

	def doSet(key:String, value:cli.SetValue) {
		if (!plan.fields.isEmpty && !plan.fields.contains(key)) warn("that is not in the plan".format(key))
		val valueString = value match {
			case cli.TextValue(value) => value
			case cli.TimeStampValue(value) => value.format
		}
		script.append(scriptmodel.Set(key, valueString, user, now)).save
	}

	def doDelete(key:String) {
		if (!scriptState.document.contains(key)) warn("That is not set")
		script.append(scriptmodel.Delete(key, user, now)).save
	}

	def doRoute(actors:Seq[String]) {
		script.append(scriptmodel.Route(actors, user, now)).save
	}

	def doPlan(name:Option[String]) {
		name match {
			case Some(name) => {
				val source = projectPlanFile(name)
				if (!source.isFile) {
					die("there is no plan file %s".format(source))
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
	}

	def doLocate(name:String) {
	  val kvs = for (name <- (name match {
	    case "all" => Seq("project", "conf", "story", "script", "plan")
	    case name => Seq(name)
	  })) yield name match {
	    case "project" => name -> projectDir
	    case "conf" => name -> projectFile
	    case "story" => name -> storyDir
	    case "script" => name -> scriptFile
	    case "plan" => name -> planFile
	    case name => die("that is not something you can locate. try p locate all, project, conf, story, script, or plan")
	  }
		if (kvs.length == 1) {
			println(kvs.head._2)
			say_ok = false // to support ` ` bash usage
		} else {
			val kw = kvs.map(_._1.length).max
			i((for ((k, v) <- kvs.sortBy(_._2)) yield "%s : %s".format(rightPad(k, kw), v.toString.bold)).mkString("\n").indent)
		}
	}

	def doAttach(files:Seq[String]) {
		files.map(new File(_)).foreach { file =>
			if (!file.isFile) warn("%s is not a file".format(file))
			else if (file.getName.endsWith(".proseeo")) warn("%s is my file and should not be attached".format(file))
			else {
				val dest = new File(storyDir, file.getName)
				if (dest.isFile) warn("%s already exists and I am overwriting it".format(dest))
				copyFile(file, dest)
				i("attached %s".format(file.getName))
			}
		}
	}
}