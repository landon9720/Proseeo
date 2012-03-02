package com.landonkuhn.proseeo.main

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical
import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._
import org.apache.commons.io.FileUtils._
import java.io.File

object ScriptParser {
  def parseScript(script: Seq[String]): Seq[Statement] = {
    val result = new ListBuffer[Statement]
    var i = 0
    while (i < script.length) {
      val line = script(i)
      var statement = ScriptStatementParser.parseStatement(line)
      statement match {
        case value: Value if value.inlineValue.isEmpty => {

          val buf = new StringBuilder
          while (i + 1 < script.length && startsWith(script(i + 1), "  ")) {
            i = i + 1
            buf.append(script(i))
            buf.append("\n")
          }
          value.multilineValue = Some(buf.toString)
        }
        case _ =>
      }
      i = i + 1
      println("parsed statement: " + statement)
      result += statement
    }
    result
  }

  def append(file: File, statement: Statement) {
    writeStringToFile(file, "\n" + statement.dsl, true)
  }
}

case class Script(statements: List[Statement])

object ScriptStatementParser {
  def parseStatement(statement: String): Statement = parser.phrase(parser.statement)(new parser.lexical.Scanner(statement)) match {
    case parser.Success(statement, _) => statement
    case e: parser.NoSuccess => Invalid("Failed understanding statement [%s]: %s".format(statement, e.msg))
  }

  private val parser = new StandardTokenParsers {

    import scala.util.parsing.input.CharArrayReader.EofCh

    override val lexical = new StdLexical {
      reserved += (
        "created", "by",
        "set",
        "route", "to", "then",
        "say"
      )
      delimiters ++= List()

      override def token: Parser[Token] =
        ( identChar ~ rep( identChar | digit )              ^^ { case first ~ rest => processIdent(first :: rest mkString "") }
        | digit ~ rep( digit )                              ^^ { case first ~ rest => NumericLit(first :: rest mkString "") }
        | '\'' ~ rep( chrExcept('\'', '\n', EofCh) ) ~ '\'' ^^ { case '\'' ~ chars ~ '\'' => StringLit(chars mkString "") }
        | '\"' ~ rep( chrExcept('\"', '\n', EofCh) ) ~ '\"' ^^ { case '\"' ~ chars ~ '\"' => StringLit(chars mkString "") }
        | EofCh                                             ^^^ EOF
        | '\'' ~> failure("unclosed string literal")
        | '\"' ~> failure("unclosed string literal")
        | delim
        | failure("illegal character")
      )

      override def identChar = letter | elem('_') | elem('.')
    }

    def statement: Parser[Statement] = created_by | set | route_to | say

    def created_by: Parser[CreatedBy] = "created" ~> "by" ~> ident ^^ {
      case user => CreatedBy(user)
    }

    def set: Parser[Set] = "set" ~> ident ~ opt(stringLit) ^^ {
      case key ~ value => Set(key, value)
    }

    def route_to: Parser[RouteTo] = "route" ~> "to" ~> ident ~ rep("then" ~> ident) ^^ {
      case user ~ next => RouteTo(user, next)
    }

    def say: Parser[SayStatement] = "say" ~> opt(stringLit) ^^ {
      case message => SayStatement(message)
    }
  }
}


trait Statement {
  def dsl: String
}

trait Value {
  def value = inlineValue.getOrElse(multilineValue.getOrElse("??"))
  val inlineValue: Option[String]
  var multilineValue: Option[String] = None
}

case class Invalid(message: String) extends Statement {
  def dsl: String = null
}
case class Set(key2: String, inlineValue: Option[String]) extends Statement with Value {
  def dsl: String = null
}

case class CreatedBy(user: String) extends Statement {
  def dsl: String = null
}

case class RouteTo(user: String, next: Seq[String]) extends Statement {
  def dsl: String = null
}

case class SayStatement(inlineValue: Option[String]) extends Statement with Value {
  def dsl: String = "say \"%s\"".format(inlineValue.getOrElse("??"))
}