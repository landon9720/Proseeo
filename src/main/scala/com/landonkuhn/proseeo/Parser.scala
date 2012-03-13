package com.landonkuhn.proseeo

import util.parsing.combinator.RegexParsers

abstract class Parser extends RegexParsers {
	def id          = "[0-9a-f]{32}".r
	def key         = "[a-zA-Z0-9._]+".r
	def text        = ".+".r
	def quotedText  = "\"" ~> "[^\"]+".r <~ "\"" ^^ { case x => x }
}
