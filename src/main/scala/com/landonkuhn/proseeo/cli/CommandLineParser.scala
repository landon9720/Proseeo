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
	// also this needs to be re-written to parse the tokens passed into main(args)
  val parser = new Parser {
    def command = (
        "help"                                                  ^^^ Help()
      | "status"                                                ^^^ Status()
      | "init" ~> name                                          ^^ { case name => Init(name) }
      | "create"                                                ^^ { case name => Start() }
      | "complete"                                              ^^^ End()
      | "use" ~> opt(id)                                        ^^ { case id => Use(id) }
      | "tell"                                                  ^^^ Tell()
      | "say" ~> text                                           ^^ { case text => Say(text) }
      | ("set" | "s") ~> force ~ key ~ opt(":" | "=") ~ text    ^^ { case force ~ key ~ _ ~ text => Set(key, text, force) }
      | ("delete" | "del") ~> key                               ^^ { case key => Delete(key) }
      | route
      | "unplan"                                                ^^^ Plan(None, false)
      | "plan" ~> force ~ opt(name)                             ^^ { case force ~ name => Plan(name, force) }
      | ("locate" | "l") ~> opt(name)                           ^^ { case name => Locate(name) }
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
case class Start() extends Command
case class End() extends Command // later create and close instead of start/end
case class Use(storyId:Option[String]) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command
case class Set(key:String, value:String, force:Boolean) extends Command
case class Delete(key:String) extends Command

trait Route extends Command
case class RouteTo(name:Actor, then:Seq[Actor]) extends Route

case class Plan(name:Option[String], force:Boolean) extends Command

case class Locate(name:Option[String]) extends Command