package com.landonkuhn.proseeo.main

import util.parsing.combinator.RegexParsers

object Proseeo {
  def main(args: Array[String]) {
    println(parseCommandLine(args.mkString(" ")))
  }

  def parseCommandLine(args: String): Command = parser.parseAll(parser.command, args) match {
    case parser.Success(command, _) => command
    case e: parser.NoSuccess => Error("Failed parsing [%s]: %s".format(args, e.msg))
  }

  private val parser = new RegexParsers {

    private implicit def string_to_regex(s: String) = s.r

    def command = help | init

    def help = regex("""\?|-\?|-h|help|-help|--help""") ^^ {
      case _ => Help()
    }

    def init = init1 | init2

    def init1 = "init" ^^ {
      case _ => Init(None)
    }

    def init2 = "init" ~ " " ~ regex(""".+""") ^^ {
      case "init" ~ " " ~ x => Init(Some(x))
      case _ => sys.error("foo")
    }
  }
}

trait Command

case class Error(message: String) extends Command
case class Help() extends Command
case class Init(dir: Option[String]) extends Command