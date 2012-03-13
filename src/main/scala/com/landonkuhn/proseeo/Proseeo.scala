package com.landonkuhn.proseeo

import access.{Project, Stories, Attachment, Plans, User, Group}

import java.io.File

import Logging._
import Ansi._
import plan._
import scriptmodel.RouteState
import Util._
import java.util.Date
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils._
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils._

object Proseeo {

	lazy val project = Project.get({
		Seq(cwd, cwd.getCanonicalFile.getParentFile).find(Project.test(_))
			.getOrElse(die("I don't see a project here. change to a project directory, or create one here using p init"))
	})

	lazy val this_user = new {
		val conf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
		val name = conf.getOrElseUpdate("user.name", System.getenv("USER"))
		lazy val useStoryName:Option[String] = {
			def projectUsing = conf.get("projects.%s.using".format(project.id))
			if (Option(cwd.getCanonicalFile.getParentFile) == Some(project.dir.getCanonicalFile)) {
				val storyId = Some(cwd.getCanonicalFile.getName)
				if (projectUsing != storyId) {
					doUse(storyId)
					warn("this is a story directory, so we are going to use the story here")
				}
			}
			projectUsing
		}
	}

	lazy val story = this_user.useStoryName match {
		case Some(storyName) => new Stories(project.dir).get(storyName)
		case None => die("I don't know what story we're using")
	}

	lazy val plans = new Plans(project.dir, story.dir)
	lazy val plan = plans.get

	var say_ok = true

	def main(args:Array[String]):Unit = try {

		args match {
			case Array("dump") => doDump
			case _ =>
		}

		val willOfTheHuman = if (args.isEmpty) {
			if (this_user.useStoryName.isDefined) cli.Tell()
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

		this_user.conf += "stats.count" -> (this_user.conf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		this_user.conf += "stats.last" -> now.format
		this_user.conf.save

		if (say_ok) ok("ok")
	} catch {
		case ex:Logging.Dying => dye_for_real(ex)
		case ex:Exception => error("sorry, I had an accident"); ex.printStackTrace
	}
	
	def doDump {
		for (file <- Seq(
			new File(new File("."), "project.proseeo").getCanonicalFile,
			new File(getUserDirectory, ".proseeo.conf").getCanonicalFile
		)) {
			println("#./%s".format(file.getName))
			println(Files.read(file).mkString("\n"))
		}
		sys.exit
	}

	def doHelp {
		i("Proseeo 0.01")
	}

	def doStatus {
		i("Users:\n" + project.users.values.mkString("\n").indent)
		i("Groups:\n" + project.groups.values.mkString("\n").indent)
	}

  def doInit(name:String) {
		val projectFile = new File(cwd, "project.proseeo")
	  if (projectFile.isFile) die("there is already a project here")
	  if (projectFile.isDirectory) die("something fishy")

	  for ((file, content) <- Seq(

		projectFile -> """
project.name: %s
project.id: %s
project.born.on: %s
project.users.lkuhn.name: Landon Kuhn
project.users.lkuhn.email: landon9720@gmail.com
project.users.example_user.name: Example User
project.users.example_user.email: user@example.com
project.groups.engineering=lkuhn
project.groups.example_group=lkuhn,example_user
""".format(name, Util.id, now.format),

	   new File(cwd, "bug.plan.proseeo") -> """
need title:text
need description:text
want version_reported_in:enum(2.2, 2.0, 1.5, 1.4, 1.3, 1.2, 1.1, 0.1_beta2, 0.1_beta1)

need scrubbed:gate
need due:timestamp

want color:enum(red, blue, green, black, pink, aqua)
""",

	   new File(cwd, "todo.plan.proseeo") -> """
need title:text
want description:text
need done:gate
""",

	   new File(cwd, "release.plan.proseeo") -> """
need version:text
want summary:text
need release_date:text
need release_document.md:text

need accepted:gate
""",

	   new File(cwd, ".gitignore") -> """
index.proseeo/
"""
	  )) {
	    Files.write(file, content.split("\n"))
	  }
	}

  def doStart(name:String) {
		val storyDir = (new File(project.dir, name) +: (for (i <- (1 to Int.MaxValue).view) yield new File(project.dir, "%s-%d".format(name, i)))).find(!_.exists).get
		val scriptFile = new File(storyDir, "script.proseeo")
	  touch(scriptFile)
		val planFile = new File(storyDir, "plan.proseeo")
		touch(planFile)
		val script = new scriptmodel.Script(scriptFile)
		script.append(scriptmodel.Created(this_user.name, now)).save
		doUse(Some(storyDir.getName))
	  if (plans.testProjectPlanFile(name)) {
		  doPlan(Some(name))
		  i("started story %s (using plan %s)".format(storyDir.getName, name))
	  } else {
			i("started story %s".format(storyDir.getName))
		}
	}

	def doEnd {
		story.script.append(scriptmodel.Ended(this_user.name, now)).save
	}

	def doUse(name:Option[String]) {
		name match {
			case Some(name) => {
				this_user.conf += "projects.%s.using".format(project.id) -> name // later check it
				this_user.conf += "projects.%s.name".format(project.id) -> project.name // nice touch
			}
			case None => {
				this_user.conf -= "projects.%s.using".format(project.id)
				this_user.conf -= "projects.%s.name".format(project.id)
			}
		}
		this_user.conf.save
	}

	def doTell {
		import StringUtils._

		val state = story.script.state
		val _ = plan // force lazy evaluation

		def atStr(date:Date) = "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date)).bold, date.when(now))

		def byStr(name:String) = project.users.get(name) match {
			case Some(User(_, fullName, email)) =>
				"%s<%s>%s".format(fullName.map(_ + " ").getOrElse("").bold, name, email.map(" " + ).getOrElse(""))
			case None => "<%s>".format(name)
		}

		def atbyStr(date:Date, name:String) = "%s by %s".format(atStr(date), byStr(name))

		val kvs =
			(List("story" -> story.dir.getName.bold)
			) ::: (state.document.get("proseeo.plan") match {
				case Some(plan) => List("plan" -> plan.bold)
				case None => List("plan" -> "no plan (use p plan name)".yellow)
			}) ::: (state.created match {
				case Some(scriptmodel.Created(by, at)) => List("created" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.ended match {
				case Some(scriptmodel.Ended(by, at)) => List("closed" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.route match {
				case RouteState(past, present, future) => List("route" -> {
					if ((past ++ present ++ future).isEmpty) "no route (use p route names)".yellow
					else past.mkString("->") + present.map("=>" + _).getOrElse("").bold + future.map("->" + _).mkString("")
				})
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

		val attachments = Attachment(story.dir)
		if (!attachments.isEmpty) {
			i("")
			val kvs = (for (attachment <- attachments.sortBy(_.fileName)) yield {
				attachment.fileName -> attachment.size
			})
			val kw = kvs.map(_._1.length).max
			i((for ((k, v) <- kvs) yield "%s  %s".format(rightPad(k, kw).bold, v)).mkString("\n"))
		}
	}

	def doSay(message:String) {
		story.script.append(scriptmodel.Say(message, this_user.name, now)).save
	}

	def doSet(key:String, value:cli.SetValue) {
		if (!plan.fields.isEmpty && !plan.fields.contains(key)) warn("that is not in the plan".format(key))
		val valueString = value match {
			case cli.TextValue(value) => value
			case cli.TimeStampValue(value) => value.format
		}
		story.script.append(scriptmodel.Set(key, valueString, this_user.name, now)).save
	}

	def doDelete(key:String) {
		if (!story.script.state.document.contains(key)) warn("That is not set")
		story.script.append(scriptmodel.Delete(key, this_user.name, now)).save
	}

	def doRoute(actors:Seq[String]) {
		story.script.append(scriptmodel.Route(actors, this_user.name, now)).save
	}

	def doPlan(name:Option[String]) {
		name match {
			case Some(name) => {
				val source = plans.projectPlanFile(name)
				if (!source.isFile || !source.canRead) {
					die("there is no plan file %s (or I can't read it)".format(source))
				}
				FileUtils.copyFile(source, plan.file)
				story.script.append(scriptmodel.Set("proseeo.plan", name, this_user.name, now)).save
			}
			case None => {
				if (plan.file.isFile) plan.file.delete
				FileUtils.touch(plan.file)
				story.script.append(scriptmodel.Delete("proseeo.plan", this_user.name, now)).save
			}
		}
	}

	def doLocate(name:String) {
	  val kvs = for (name <- (name match {
	    case "all" => Seq("project", "conf", "story", "script", "plan")
	    case name => Seq(name)
	  })) yield name match {
	    case "project" => name -> project.dir
	    case "conf" => name -> project.file
	    case "story" => name -> story.dir
	    case "script" => name -> story.script.file
	    case "plan" => name -> plan.file
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
				val dest = new File(story.dir, file.getName)
				if (dest.isFile) warn("%s already exists and I am overwriting it".format(dest))
				copyFile(file, dest)
				i("attached %s".format(file.getName))
			}
		}
	}
}