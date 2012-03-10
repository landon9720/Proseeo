package com.landonkuhn.proseeo.scriptmodel

import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import com.landonkuhn.proseeo._
import Logging._
import Files._
import scriptmodel.ScriptStatementParser._

class Script(file:File) {

	def append(statement:Statement):Script = {
		statements += statement
		this
	}

	def save:Script = {
		write(file, statements.map(_.toString))
		this
	}

	def play:State = {
		var created:Option[Created] = None
		var ended:Option[Ended] = None
		val says = new ListBuffer[Say]
		val document = new Document
		var where:Option[String] = None

		for (statement <- statements) statement match {
			case c:Created => if (created.isDefined) die("More than one created") else created = Some(c)
			case e:Ended => if (ended.isDefined) die("More than one ended") else ended = Some(e)
			case s:Say => says += s
			case Set(key, value, _, _) => document += key -> value
			case Delete(key, _, _) => document -= key
			case Route(actors, _, _) => where = Some(actors.head)
		}

		State(created, ended, says, document, where)
	}

	private val statements:ListBuffer[Statement] = {
		val result = new ListBuffer[Statement]
		result ++= (for (line <- read(file).map(trim) if line.length > 0 && !startsWith(line, "#")) yield {
			parseScriptStatement(line)
		})
		result
	}
}

case class State(
	created:Option[Created],
	ended:Option[Ended],
	says:Seq[Say],
	document:Document,
	where:Option[String]
)