package com.landonkuhn.proseeo.cli

import com.landonkuhn.proseeo._
import Logging._

object CommandLineParser {

	def parseCommandLine(args:String):Command = {
    parser.parse(parser.command, args) match {
      case parser.Success(command, _) => command
      case e:parser.NoSuccess => die("I don't understand [%s]".format(args), e.msg)
    }
  }

  val parser = new Parser {
    def command = (
        "help"              ^^^ Help()
      | "status"            ^^^ Status()
      | "init" ~> ".+".r    ^^ { case name => Init(name) }
      | "start"             ^^^ Start()
      | "end"               ^^^ End()
      | "use" ~> id         ^^ { case id => Use(id) }
      | "tell"              ^^^ Tell()
      | "say" ~> text       ^^ { case text => Say(text) }
      | "set" ~> key ~ text ^^ { case key ~ text => Set(key, text) }
      | route
     )
    def route = "route" ~> "to" ~> actor ~ rep("then" ~> actor) ^^ { case name ~ then => RouteTo(name, then) }
  }
}

trait Command
case class Help() extends Command
case class Status() extends Command
case class Init(name:String) extends Command
case class Start() extends Command
case class End() extends Command
case class Use(storyId:String) extends Command
case class Tell() extends Command
case class Say(message:String) extends Command
case class Set(key:String, value:String) extends Command

trait Route extends Command
case class RouteTo(name:Actor, then:Seq[Actor]) extends Route