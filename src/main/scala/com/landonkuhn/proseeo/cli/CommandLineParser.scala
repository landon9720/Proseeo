package com.landonkuhn.proseeo.cli

import com.landonkuhn.proseeo._
import Logging._
import scriptmodel.Unplan

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
      | "create" ~> opt(name)                                   ^^ { case name => Start(name) }
      | "complete"                                              ^^^ End()
      | "use" ~> opt(id)                                        ^^ { case id => Use(id) }
      | "tell"                                                  ^^^ Tell()
      | "say" ~> text                                           ^^ { case text => Say(text) }
      | ("set" | "s") ~> force ~ key ~ opt(":" | "=") ~ text    ^^ { case force ~ key ~ _ ~ text => Set(key, text, force) }
      | ("delete" | "del") ~> key                               ^^ { case key => Delete(key) }
      | route
      | "unplan"                                                ^^^ Plan(None, false)
      | "plan" ~> force ~ opt(name)                             ^^ { case force ~ name => Plan(name, force) }
      | "x" ~> text                                             ^^ { case cmd => Cmd(cmd) }
      | "cat" ~ "script"                                        ^^^ CatScript()
      | "cat" ~ "plan"                                          ^^^ CatPlan()
      | "edit" ~ "script"                                       ^^^ EditScript()
      | "edit" ~ "plan" ~> global                               ^^ { case global => EditPlan(global) }
     )
    def force = opt("--force" | "-f") ^^ { case force => force.isDefined }
    def route = "route" ~> "to" ~> actor ~ rep("then" ~> actor) ^^ { case name ~ then => RouteTo(name, then) }
    def global = opt("--global") ^^ { case global => global.isDefined }
  }
}

trait Command
case class Help() extends Command
case class Status() extends Command
case class Init(name:String) extends Command
case class Start(plan:Option[String]) extends Command
case class End() extends Command // later create and close instead of start/end
case class Use(storyId:Option[String]) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command
case class Set(key:String, value:String, force:Boolean) extends Command
case class Delete(key:String) extends Command

trait Route extends Command
case class RouteTo(name:Actor, then:Seq[Actor]) extends Route

case class Plan(name:Option[String], force:Boolean) extends Command

case class Cmd(cmd:String) extends Command

case class CatScript() extends Command
case class CatPlan() extends Command
case class EditScript() extends Command
case class EditPlan(global:Boolean) extends Command