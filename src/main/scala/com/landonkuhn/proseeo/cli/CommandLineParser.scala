package com.landonkuhn.proseeo.cli

import com.landonkuhn.proseeo._
import Logging._

object CommandLineParser {

	def parseCommandLine(args:String):Command = {
    parser.parseAll(parser.command, args) match {
      case parser.Success(command, _) => command
      case e:parser.NoSuccess => die("I don't understand [%s]\n  %s".format(args, e.msg))
    }
  }

	// later, there is a problem here and with the other parsers
	// for example: p story foo is parsed as s[et] tory foo
  val parser = new Parser {
    def command = (
        "help"                                                  ^^^ Help()
      | "status"                                                ^^^ Status()
      | "init" ~> name                                          ^^ { case name => Init(name) }
      | "start" ~> opt(name)                                    ^^ { case name => Start(name) }
      | "end"                                                   ^^^ End()
      | "use" ~> id                                             ^^ { case id => Use(id) }
      | "tell"                                                  ^^^ Tell()
      | "say" ~> text                                           ^^ { case text => Say(text) }
      | ("set" | "s") ~> force ~ key ~ opt(":" | "=") ~ text    ^^ { case force ~ key ~ _ ~ text => Set(key, text, force) }
      | ("delete" | "del") ~> key                               ^^ { case key => Delete(key) }
      | route
      | "plan" ~> force ~ name                                  ^^ { case force ~ name => Plan(name, force) }
     )
    def force = opt("--force" | "-f") ^^ { case force => force.isDefined }
    def route = "route" ~> "to" ~> actor ~ rep("then" ~> actor) ^^ { case name ~ then => RouteTo(name, then) }
  }
}

trait Command
case class Help() extends Command
case class Status() extends Command
case class Init(name:String) extends Command
case class Start(plan:Option[String]) extends Command
case class End() extends Command // later create and close instead of start/end
case class Use(storyId:String) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command
case class Set(key:String, value:String, force:Boolean) extends Command
case class Delete(key:String) extends Command

trait Route extends Command
case class RouteTo(name:Actor, then:Seq[Actor]) extends Route

case class Plan(name:String, force:Boolean) extends Command