package com.landonkuhn.proseeo.plan

import java.io.File
import org.apache.commons.lang3.StringUtils._

import com.landonkuhn.proseeo._
import Logging._
import Util._
import Files._
import Ansi._
import PlanLineParser._
import scriptmodel.State

class Plan(val name:String, file:File) {

	def apply(state:State) {
		var future = false
		for (group <- groups) {

			val active = ! group.forall(_.test(state.document))

			for (field <- group) {
				val prefix = field match {
					case w@Want(key, kind) if !w.test(state.document) => "want"
					case n@Need(Want(key, kind)) if !n.test(state.document) => "need"
					case _ => "   "
				}
				val value = field.kind match {
					case g:Gate if field.test(state.document) => "[*]"
					case _ => state.document.get(field.key).getOrElse("")
				}
				val hint = field match {
					case field:Field if field.test(state.document) => ""
					case field:Field => field.kind match {
						case _:Text => "____"
						case Enum(values) => values.take(4).mkString(", ") + (values.drop(4).size match {
							case 0 => ""
							case n => ", %d more".format(n)
						})
						case _:Gate => "[ ]"
					}
				}
				val cursor = if (active && !future && !field.test(state.document)) "> " else "  "
				info("%s %s %s %s %s".format(cursor, prefix, field.key, value.bold, hint.yellow))
			}
			if (active) future = true
			info("")
		}
	}

	override def toString = (for (group <- groups) yield {
		(for (line <- group) yield line.toString).mkString("\n")
	}).mkString("\n\n")

	private val groups:Seq[Seq[Field]] = {
		def f(fields:Seq[Line]):Seq[Seq[Field]] = {
			// i would love to know a better way to do this
			val (h:Seq[Field], t:Seq[Line]) = fields.span(_.isInstanceOf[Field])
			h +: (t.toList match {
				case Nil => Nil
				case t => f(t.dropWhile(!_.isInstanceOf[Field]))
			})
		}
		f((for (line <- read(file).map(trim) if !startsWith(line, "#")) yield parseLine(line)).dropWhile(!_.isInstanceOf[Field]))
	}

	val fields = groups.flatten.map(field => field.key -> field).toMap
}

trait Line

case class NilLine() extends Line {
	override def toString = ""
}
trait Field extends Line {
	def key:String
	def kind:Kind
	def test(document:Document):Boolean
}
case class Want(key:String, kind:Kind) extends Field {
	override def toString = "want %s:%s".format(key, kind)
	def test(document:Document):Boolean = kind.test(document, key)
}
case class Need(want:Want) extends Field {
	override def toString = "need %s:%s".format(want.key, want.kind)
	val key = want.key
	val kind = want.kind
	def test(document:Document):Boolean = want.test(document)
}

trait Kind {
	def test(document:Document, key:String):Boolean
}
case class Text() extends Kind { // later rename to any?
	override def toString = "text"
	def test(document:Document, key:String):Boolean = document.contains(key)
}
case class Enum(values:Seq[String]) extends Kind {
	override def toString = "enum(%s)".format(values.mkString(" "))
	def test(document:Document, key:String):Boolean = document.get(key).map(value => values.exists(_ == value)).getOrElse(false)
}
case class Gate() extends Kind { // later rename to checkpoint?
	override def toString = "gate"
	def test(document:Document, key:String):Boolean = document.get(key).map(_ == "ok").getOrElse(false)
}

object PlanLineParser {
	def parseLine(line:String):Line = {
		if (trim(line) == "") NilLine() else parser.parse(parser.line, line) match {
			case parser.Success(line, _) => line
			case e:parser.NoSuccess => die("Sorry, but I don't understand something in here: [%s]".format(line), e.msg)
		}
	}

	val parser = new Parser {
		def line:Parser[Line] = (
			 "want" ~> want
			| "need" ~> want ^^ { case want => Need(want) }
		)
		def want:Parser[Want] = (key ~ ":" ~ kind) ^^ { case k ~ ":" ~ l => Want(k, l) }
		def kind:Parser[Kind] = (
			  "text" ^^^ Text()
			| "enum(" ~> rep1sep(key, ("," | " ")) <~ ")" ^^ { case values => Enum(values) }
			| "gate"  ^^^ Gate()
		)
	}
}