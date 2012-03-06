package com.landonkuhn.proseeo.plan

import java.io.File
import org.apache.commons.lang3.StringUtils._

import com.landonkuhn.proseeo._
import Logging._
import Util._
import Files._
import PlanLineParser._

class Plan(file: File) {

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
}

trait Line

case class NilLine() extends Line {
	override def toString = ""
}
trait Field extends Line
case class Want(key:String, kind:Kind) extends Field {
	override def toString = "want %s:%s".format(key, kind)
}
case class Need(want:Want) extends Field {
	override def toString = "need %s:%s".format(want.key, want.kind)
}

trait Kind
case class Text() extends Kind {
	override def toString = "text"
}
case class Enum(name:String) extends Kind {
	override def toString = "enum(%s)".format(name)
}
case class Gate() extends Kind {
	override def toString = "gate"
}

object PlanLineParser {
	def parseLine(line:String):Line = {
		info("i am parsing " + line)
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
			| "enum(" ~> name <~ ")" ^^ { case name => Enum(name) }
			| "gate"  ^^^ Gate()
		)
	}
}