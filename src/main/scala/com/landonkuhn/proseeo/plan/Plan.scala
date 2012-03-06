package com.landonkuhn.proseeo.plan

import java.io.File
import org.apache.commons.lang3.StringUtils._

import com.landonkuhn.proseeo._
import Logging._
import Util._
import Files._
import PlanLineParser._

class Plan(file: File) {

	val groups:Seq[Seq[Field]] = {
		def f(fields:Seq[Line]):Seq[Seq[Field]] = {
			val (h, t) = fields.span(_.isInstanceOf[Field])
			info("(h, t): " + (h.size, t.size))
			List(h.toList) :: t.toList match {
				case Nil => Nil
				case t => f(t.dropWhile(!_.isInstanceOf[Field]))
			}
		}
		f(for (line <- read(file).map(trim) if !startsWith(line, "#")) yield parseLine(line))
	}
}

trait Line

case class NilLine() extends Line
trait Field extends Line
case class Want(key:String, kind:Kind) extends Field
case class Need(want:Want) extends Field

trait Kind
case class Text() extends Kind
case class Enum(name:String) extends Kind
case class Gate() extends Kind

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