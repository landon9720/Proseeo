package com.landonkuhn.proseeo.template

import java.io.File
import org.apache.commons.lang3.StringUtils._

import com.landonkuhn.proseeo._
import Logging._
import Util._
import Files._
import TemplateLineParser._

class Template(file: File) {

	val groups:Seq[Seq[Field]] = {
		def f(fields:Seq[Line]):Seq[Seq[Field]] = {
			val (h, t) = fields.span(_.isInstanceOf[Field])
			h.toList ::: t.toList match {
				case Nil => Nil
				case t => f(t.dropWhile(!_.isInstanceOf[Field]))
			}
		}
		f(for (line <- read(file).map(trim) if !startsWith(line, "#")) yield parseLine(line))
	}
}

trait Line

object NilLine extends Line
trait Field extends Line
case class Want(key:String, kind:Kind) extends Field
case class Need(want:Want) extends Field

trait Kind
object Text extends Kind
case class Enum(name:String) extends Kind
object Gate extends Kind

object TemplateLineParser {
	def parseLine(line:String):Line = {
		if (trim(line) == "") NilLine else parser.parse(parser.line, line) match {
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
			  "text" ^^^ Text
			| "enum(" ~> name <~ ")" ^^ { case name => Enum(name) }
			| "gate"  ^^^ Gate
		)
	}
}