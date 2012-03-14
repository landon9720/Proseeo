package com.landonkuhn.proseeo

import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import Logging._
import Files._
import java.util.Date

class Script(val file:File) {

	def append(statement:Statement):Script = {
		statements += statement
		this
	}

	def replace[T <: Statement](existing:T, replacement:T):Script = {
		statements.indexOf(existing) match {
			case -1 => die("failed updating script")
			case i => statements.update(i, replacement)
		}
		this
	}

	def save:Script = {
		write(file, statements.map(_.toString))
		this
	}

	val statements:ListBuffer[Statement] = {
		// later how to make this private?
		val result = new ListBuffer[Statement]
		result ++= (for (line <- read(file).map(trim) if line.length > 0 && !startsWith(line, "#")) yield {
			ScriptStatementParser.parseScriptStatement(line)
		})
		result
	}

	lazy val state:State = {
		var created:Option[Created] = None
		var ended:Option[Ended] = None
		var touched:Option[Date] = None
		val says = new ListBuffer[Say]
		val document = new Document
		var route = RouteState(Nil, None, Nil)

		for (statement <- statements) {
			statement match {
				case c:Created => if (created.isDefined) die("More than one created") else {
					created = Some(c)
					route = RouteState(Nil, Option(c.by), Nil)
				}
				case e:Ended => if (ended.isDefined) die("More than one ended") else ended = Some(e)
				case s:Say => says += s
				case Set(key, value, _, _) => document += key -> value
				case Delete(key, _, _) => document -= key
				case Route(actors, _, _) => route = route.copy(
					past = route.past ++ {
						if (route.past.lastOption != route.present && route.present != actors.headOption)
							route.present.map(Seq(_)).getOrElse(Nil)
						else Nil
					},
					present = actors.headOption,
					future = actors.drop(1)
				)
			}
			touched = Some(statement.at)
		}

		State(created, ended, touched, says, document, route)
	}
}

case class State(
	created:Option[Created],
	ended:Option[Ended],
	touched:Option[Date],
	says:Seq[Say],
	document:Document,
	route:RouteState
)

case class RouteState(
	past:Seq[String],
	present:Option[String],
	future:Seq[String]
)