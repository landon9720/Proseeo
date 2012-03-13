package com.landonkuhn.proseeo.cli

import com.eaio.util.text.HumanTime
import org.joda.time.DateTime
import java.util.Date

import com.landonkuhn.proseeo._
import Logging._
import Util._

object CommandLineParser {
	def parseCommandLine(args:List[String]):Command = {
    args match {
	    case "help" :: Nil => Help()
	    case "status" :: Nil => Status()
	    case "init" :: name :: Nil => Init(name)
	    case "create" :: name :: Nil => Start(name)
	    case "start" :: name :: Nil => Start(name)
	    case "close" :: Nil => End()
	    case "end" :: Nil => End()
	    case "use" :: name :: Nil => Use(Some(name))
	    case "use" :: Nil => Use(None)
	    case "tell" :: Nil => Tell()
	    case "t" :: Nil => Tell()
		  case "say" :: tail :: Nil => Say(tail.mkString(""))
		  case "set" :: key :: "now" :: Nil => Set(key, TimeStampValue(now))
		  case "set" :: key :: delta :: Nil if HumanTime.eval(delta).getDelta != 0L => Set(key, TimeStampValue(new DateTime().plus(HumanTime.eval(delta).getDelta).toDate))
		  case "set" :: key :: value :: Nil=> Set(key, TextValue(value))
			case "delete" :: key :: Nil => Delete(key)
	    case "ask" :: actor :: Nil => Ask(actor)
	    case "pass" :: Nil => Pass()
	    case "route" :: actor :: actors => RouteInsert((actor :: actors).filter(_ != "to"))
	    case "append" :: actor :: actors => RouteAppend((actor :: actors).filter(_ != "to"))
	    case "reroute" :: actor :: actors => Reroute((actor :: actors).filter(_ != "to"))
	    case "plan" :: name :: Nil => Plan(Some(name))
	    case "unplan" :: Nil => Plan(None)
	    case "locate" :: Nil => Locate("all")
	    case "locate" :: name :: Nil => Locate(name)
	    case "attach" :: files => Attach(files)
	    case _ => die("I don't understand you")
    }
  }
}

trait Command

case class Help() extends Command
case class Status() extends Command
case class Init(name:String) extends Command
case class Start(name:String) extends Command
case class End() extends Command
case class Use(name:Option[String]) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command

trait SetValue
case class TextValue(value:String) extends SetValue
case class TimeStampValue(value:Date) extends SetValue
case class Set(key:String, value:SetValue) extends Command
case class Delete(key:String) extends Command

trait Route extends Command
case class RouteInsert(actors:Seq[String]) extends Route
case class RouteAppend(actors:Seq[String]) extends Route
case class Reroute(actors:Seq[String]) extends Route
case class Ask(actor:String) extends Route
case class Pass() extends Route

case class Plan(name:Option[String]) extends Command
case class Locate(name:String) extends Command
case class Attach(files:Seq[String]) extends Command
