package com.landonkuhn.proseeo.cli

import com.landonkuhn.proseeo._
import Logging._

object CommandLineParser {
	def parseCommandLine(args:List[String]):Command = {
    args match {
	    case "help" :: Nil => Help()
	    case "status" :: Nil => Status()
	    case "init" :: name :: Nil => Init(name)
	    case "create" :: Nil => Start()
	    case "start" :: Nil => Start()
	    case "complete" :: Nil => End()
	    case "end" :: Nil => End()
	    case "tell" :: Nil => Tell()
	    case "t" :: Nil => Tell()
		  case "say" :: tail :: Nil => Say(tail.mkString(""))
		  case "set" :: key :: value :: Nil=> Set(key, value.mkString(""))
			case "delete" :: key :: Nil => Delete(key)
	    case "route" :: actor :: then => RouteTo(actor :: then)
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
case class Start() extends Command
case class End() extends Command // later create and close instead of start/end
case class Use(storyId:Option[String]) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command
case class Set(key:String, value:String) extends Command
case class Delete(key:String) extends Command

trait Route extends Command
case class RouteTo(actors:Seq[String]) extends Route

case class Plan(name:Option[String]) extends Command

case class Locate(name:String) extends Command
case class Attach(files:Seq[String]) extends Command