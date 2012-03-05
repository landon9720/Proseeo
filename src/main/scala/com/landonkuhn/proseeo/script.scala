package com.landonkuhn.proseeo

import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import Logging._
import Files._
import com.landonkuhn.proseeo.ScriptStatementParser.Statement
import Util._

class Script(file:File) {

	def append(statement:Statement):Script = {
		statements += statement
		this
	}

	def save:Script = {
		write(file, statements.map(_.toString))
		this
	}

	def play {
		info("play")
		for (statement <- statements) info(statement.toString)
	}

//	def play:State = {
//		val document = new Document
//		val history = new ListBuffer[String]
//		var current:Option[Route] = None
//		val comments = new ListBuffer[String]
//
//		for (statement <- statements) statement match {
//			case c@Created() => {
//
//			}
//			case set@Set(key, _) => document += key -> set.value
//			case route@Route(user, next) => {
//				history += user
//				current = Some(route)
//			}
//			case say:Say => comments += say.value
//			case x => die("I don't know about: " + x)
//		}

//		State(document, history, current, comments)
//	}

//	case class State(document:Document, stack:Seq[String], current:Option[Route], comments:Seq[String])

	private val statements:ListBuffer[Statement] = {
		val result = new ListBuffer[Statement]
		result ++= (for (line <- read(file).map(trim) if line.length > 0 && !startsWith(line, "#")) yield {
			ScriptStatementParser.parseScriptStatement(line)
		})
		result
	}
}