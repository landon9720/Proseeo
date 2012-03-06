package com.landonkuhn.proseeo

import util.parsing.combinator.RegexParsers

abstract class Parser extends RegexParsers {
	def id          = "[0-9a-f]{32}".r
	def key         = "[a-zA-Z0-9._]+".r
	def text        = ".+".r
	def quotedText  = "\"" ~> "[^\"]+".r <~ "\"" ^^ { case x => x }
	def name        = "[a-zA-Z0-9]+".r ^^ { case x => x }

	def actor = (
		  "group" ~> name     ^^ { case name => GroupActor(name) }
		| opt("user") ~> name ^^ { case name => UserActor(name) }
	)
}

trait Actor

case class UserActor(name:String) extends Actor {
	override def toString = "user %s".format(name)
}

case class GroupActor(name:String) extends Actor {
	override def toString = "group %s".format(name)
}