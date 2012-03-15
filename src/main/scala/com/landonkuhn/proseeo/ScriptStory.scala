package com.landonkuhn.proseeo

import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import java.io.File

import Logging._
import Files._

class ScriptStory(val name:String, val file:File, val dir:File) extends Story {

	def append(statement:Statement):ScriptStory = {
		statements += statement
		this
	}

	def replace[T <: Statement](existing:T, replacement:T):ScriptStory = {
		statements.indexOf(existing) match {
			case -1 => die("failed updating script")
			case i => statements.update(i, replacement)
		}
		this
	}

	def save:ScriptStory = {
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

	val (plan, created, ended, tail, says, document, route) = {
		var _plan:Option[String] = None
		var _created:Option[Created] = None
		var _ended:Option[Ended] = None
		var _tail:Option[Statement] = None
		var _says = new ListBuffer[Say]
		var _document = new Document
		var _route = RouteState(Nil, None, Nil)
		for (statement <- statements) {
			statement match {
				case c:Created => if (_created.isDefined) die("More than one created") else {
					_created = Some(c)
					_route = RouteState(Nil, Option(c.by), Nil)
				}
				case e:Ended => if (_ended.isDefined) die("More than one ended") else _ended = Some(e)
				case s:Say => _says += s
				case Set(key, value, _, _) => _document += key -> value
				case Delete(key, _, _) => _document -= key
				case Route(actors, _, _) => _route = _route.copy(
					past = _route.past ++ {
						if (_route.past.lastOption != _route.present && _route.present != actors.headOption)
							_route.present.map(Seq(_)).getOrElse(Nil)
						else Nil
					},
					present = actors.headOption,
					future = actors.drop(1)
				)
			}
			_tail = Some(statement)
		}
		(_document.get("proseeo.plan"), _created, _ended, _tail, _says, _document.view((k, _) => !k.startsWith("proseeo.")), _route)
	}
}
