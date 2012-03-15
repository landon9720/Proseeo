package com.landonkuhn.proseeo

import access.{Project, Stories, Attachment, Plans, User, Group}

import java.io.File

import Logging._
import Ansi._
import Util._
import java.util.Date
import org.joda.time.format.DateTimeFormat
import org.joda.time.DateTime
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.StringUtils._
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils._
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{MatchAllDocsQuery, IndexSearcher}

object Proseeo {

	lazy val project = Project.get({
		Seq(cwd, cwd.getCanonicalFile.getParentFile).find(Project.test(_))
			.getOrElse(die("I don't see a project here. change to a project directory, or create one here using p init"))
	})
	lazy val stories = new Stories(project)

	lazy val this_user = new {
		val conf = new Conf(new File(getUserDirectory, ".proseeo.conf"))
		val name = conf.getOrElseUpdate("user.name", System.getenv("USER"))
		lazy val useStoryName:Option[String] = {
			def projectUsing = conf.get("projects.%s.using".format(project.id))
			if (Option(cwd.getCanonicalFile.getParentFile) == Some(project.dir.getCanonicalFile)) {
				val storyId = Some(cwd.getCanonicalFile.getName)
				if (projectUsing != storyId) {
					use(storyId)
					warn("this is a story directory, so we are going to use the story here")
				}
			}
			projectUsing.filter(projectUsing => {
				val t = stories.test(projectUsing)
				if (!t) {
					use(None)
					warn("no longer using story %s".format(projectUsing))
				}
				t
			})
		}
	}

	lazy val story = this_user.useStoryName match {
		case Some(storyName) => stories.get(storyName)
		case None => die("I don't know what story we're using")
	}

	lazy val plans = new Plans(project.dir, story.dir)
	lazy val plan = plans.get

	var say_ok = true

	def main(args:Array[String]):Unit = try {
		args.toList match {
			case List("dump") => dump
			case List() if (this_user.useStoryName.isDefined) => tell
			case List() => status // later reports
			case "help" :: Nil => help
			case "status" :: Nil => status
			case "init" :: name :: Nil => init(name)
			case "new" :: name :: Nil => start(name)
			case "create" :: name :: Nil => start(name)
			case "start" :: name :: Nil => start(name)
			case "close" :: Nil => end
			case "end" :: Nil => end
			case "use" :: name :: Nil => use(Some(name))
			case "use" :: Nil => use(None)
			case "tell" :: Nil => tell
			case "say" :: tail :: Nil => say(tail.mkString(""))
			case "set" :: key :: "now" :: Nil => set(key, now.format)
			case "set" :: key :: delta :: Nil if delta.toDuration != 0L => set(key, new Date(now.getTime + delta.toDuration).format)
			case "set" :: key :: value :: Nil=> set(key, value)
			case "delete" :: key :: Nil => delete(key)
			case "ask" :: actor :: Nil => ask(actor)
			case "pass" :: Nil => pass
			case "route" :: actor :: actors => routeInsert((actor :: actors).filter(_ != "to"))
			case "append" :: actor :: actors => routeAppend((actor :: actors).filter(_ != "to"))
			case "reroute" :: actor :: actors => reroute((actor :: actors).filter(_ != "to"))
			case "plan" :: name :: Nil => plan(Some(name))
			case "unplan" :: Nil => plan(None)
			case "locate" :: Nil => locate("all")
			case "locate" :: name :: Nil => locate(name)
			case "attach" :: files => attach(files)
			case "whois" :: actor :: Nil => whois(actor)
			case "index" :: Nil => index
			case "report" :: Nil => report
			case _ => die("I didn't understand that at all: " + args.mkString(" "))
		}

		this_user.conf += "stats.count" -> (this_user.conf.get("stats.count").getOrElse("0").optLong.getOrElse(0L) + 1L).toString
		this_user.conf += "stats.last" -> now.format
		this_user.conf.save

		if (say_ok) ok(if (warned) "but ok" else "ok")
	} catch {
		case ex:Logging.Dying => dye_for_real(ex)
		case ex:Exception => error("sorry, I had an accident"); ex.printStackTrace
	}
	
	def dump {
		for (file <- Seq(
			new File(new File("."), "project.proseeo").getCanonicalFile,
			new File(getUserDirectory, ".proseeo.conf").getCanonicalFile
		)) {
			println("#./%s".format(file.getName))
			println(Files.read(file).mkString("\n"))
		}
		sys.exit
	}

	def help {
		i("Proseeo 0.01")
	}

	def status {
		i("Users:\n" + project.users.values.mkString("\n").indent)
		i("Groups:\n" + project.groups.values.mkString("\n").indent)
	}

  def init(name:String) {
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

  def start(name:String) {
		val storyDir = (new File(project.dir, name) +: (for (i <- (1 to Int.MaxValue).view) yield new File(project.dir, "%s-%d".format(name, i)))).find(!_.exists).get
		val scriptFile = new File(storyDir, "script.proseeo")
	  touch(scriptFile)
		val planFile = new File(storyDir, "plan.proseeo")
		touch(planFile)
		val script = new Script(scriptFile)
		script.append(Created(this_user.name, now)).save
		use(Some(storyDir.getName))
	  if (plans.testProjectPlanFile(name)) {
		  plan(Some(name))
		  i("started story %s (using plan %s)".format(storyDir.getName, name))
	  } else {
			i("started story %s".format(storyDir.getName))
		}
	}

	def end {
		story.script.append(Ended(this_user.name, now)).save
	}

	def use(name:Option[String]) {
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

	def tell {
		import StringUtils._

		val state = story.script.state
		val _ = plan // force lazy evaluation

		def atStr(date:Date) = "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date)).bold, date.when)

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
				case Some(Created(by, at)) => List("created" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.ended match {
				case Some(Ended(by, at)) => List("closed" -> atbyStr(at, by))
				case None => Nil
			}) ::: (state.route match {
				case RouteState(past, present, future) => List("route" -> {
					if ((past ++ present ++ future).isEmpty) "no route (use p route names)".yellow
					else {
						def annotate(n:String) = if (project.isGroup(n)) {
							"*" + n + "*"
						} else n
						past.map(annotate).mkString("->") + present.map(annotate).map("=>" + _).getOrElse("").bold + future.map("->" + annotate(_)).mkString("")
					}
				})
			}) ::: (state.touched match {
				case Some(touched) => List("touched" -> touched.when)
				case None => Nil
			}) ::: Nil
		val kw = if (kvs.isEmpty) 0 else kvs.map(_._1.length).max
		i((for ((k, v) <- kvs) yield "%s : %s".format(rightPad(k, kw), v)).mkString("\n"))

		for (say <- state.says) {
			i("")
			i("%s\n  by %s\n  %s".format(say.text.bold, byStr(say.by), say.at.when))
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
						Some("%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss").print(new DateTime(date)).bold, date.when))
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
					case Some(date) => "%s %s".format(DateTimeFormat.forPattern("yyyy-MM-dd hh:mm:ss").print(new DateTime(date)).bold, date.when)
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

	def say(message:String) {
		def amend(say0:String, say1:String):Boolean = (
			((getLevenshteinDistance(say0, say1) / ((say0.length + say1.length) / 2.0d)) < 0.1d)
			|| say1.startsWith(say0)
			|| say1.endsWith(say0)
		)
		val say = Say(message, this_user.name, now)
		story.script.statements.collect({ case x@Say(text, this_user.name, _) => x }).lastOption match {
			case Some(last) if amend(last.text, message) => {
				warn("(amending your last say)")
				story.script.replace(last, say)
			}
			case _=> story.script.append(say)
		}
		story.script.save
	}

	def set(key:String, value:String) {
		if (!plan.fields.isEmpty && !plan.fields.contains(key)) warn("(that is not in the plan)".format(key))
		story.script.append(Set(key, value, this_user.name, now)).save
	}

	def delete(key:String) {
		if (!story.script.state.document.contains(key)) die("(that is not set)")
		story.script.append(Delete(key, this_user.name, now)).save
	}

	lazy val previousRoute = story.script.state.route

	def ask(actor:String) {
		doRoute((actor +: previousRoute.present.getOrElse(this_user.name) +: previousRoute.future).dedupe)
	}
	def pass {
		if (previousRoute.present.isEmpty) die("this story doesn't have anywhere to go")
		else doRoute(previousRoute.future)
	}
	def routeInsert(actors:Seq[String]) {
		doRoute((actors ++ previousRoute.future).dedupe)
	}
	def routeAppend(actors:Seq[String]) {
		doRoute((previousRoute.future ++ actors).dedupe)
	}
	def reroute(actors:Seq[String]) {
		doRoute(actors.dedupe)
	}
	def doRoute(actors:Seq[String]) {
		for (actor <- (previousRoute.future.toSet -- actors)) {
			warn("%s has been lost from the route".format(actor))
		}
		for (actor <- actors if project.actor(actor) == None) {
			warn("%s is not in the project".format(actor))
		}
		story.script.append(Route(actors, this_user.name, now)).save
	}

	def plan(name:Option[String]) {
		name match {
			case Some(name) => {
				val source = plans.projectPlanFile(name)
				if (!source.isFile || !source.canRead) {
					die("there is no plan file %s (or I can't read it)".format(source))
				}
				FileUtils.copyFile(source, plan.file)
				story.script.append(Set("proseeo.plan", name, this_user.name, now)).save
			}
			case None => {
				if (plan.file.isFile) plan.file.delete
				FileUtils.touch(plan.file)
				story.script.append(Delete("proseeo.plan", this_user.name, now)).save
			}
		}
	}

	def locate(name:String) {
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

	def attach(files:Seq[String]) {
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

	def whois(actor:String) {
		project.actor(actor) match {
			case Some(Left(User(userName, fullName, email))) => i("%s<%s>%s".format(fullName.map(_ + " ").getOrElse(""), userName, email.map(" " + _).getOrElse("")))
			case Some(Right(Group(groupName, members))) => for (user <- members) whois(user.userName)
			case None => warn("unknown: %s".format(actor))
		}
	}

	val lucene = new {
		val dir = FSDirectory.open(new File("index.proseeo"))
		val analyzer = new StandardAnalyzer(Version.LUCENE_35)
	}

	def index {
		import org.apache.lucene.document.{Document=>D, Field=>F}
		val iwc = new IndexWriterConfig(Version.LUCENE_35, lucene.analyzer)
		iwc.setOpenMode(OpenMode.CREATE)
		val writer = new IndexWriter(lucene.dir, iwc)
		val stories = new Stories(project)
		for (story <- stories.all) {
			val state = story.script.state
			writer.addDocument {
				val d = new D
				d.add(
					new F("story.name", story.name, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
				for (created <- state.created) {
					d.add(new F("story.script.created.at", created.at.format, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
					d.add(new F("story.script.created.by", created.by, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
				}
				println(d.toString)
				d
			}
		}

		writer.close()
	}

	def report {
		import org.apache.lucene.document.{Document=>D, Field=>F}
		val reader = IndexReader.open(lucene.dir, true)
		val searcher = new IndexSearcher(reader)
		val query = new MatchAllDocsQuery
		val docs = searcher.search(query, 100)
		println("totalHits " + docs.totalHits)
		for (doc <- docs.scoreDocs) {
			println(doc.score)
			import collection.JavaConversions._
			val map = (Map[String, String]() /: reader.document(doc.doc).getFields) { (map, f) =>
				map + (f.name -> f.stringValue)
			}
			println(new Document(map).toString)
		}
		searcher.close
		reader.close
	}
}