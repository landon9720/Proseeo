package com.landonkuhn.proseeo.main

import util.parsing.combinator.syntactical.StandardTokenParsers
import util.parsing.combinator.lexical.StdLexical
import collection.mutable.ListBuffer
import org.apache.commons.lang3.StringUtils._

object ScriptParser {
  def parseScript(script: Seq[String]): Seq[Statement] = {
    val result = new ListBuffer[Statement]
    var i = 0
    while (i < script.length) {
      val line = script(i)
      ScriptStatementParser.parseStatement(line) match {
        case Set(key3, None) => {
          i = i + 1
          val buf = new StringBuilder
          while (startsWith(script(i), "  ")) {
            buf.append(script(i))
            buf.append("\n")
            i = i + 1
          }
          result += Set(key3, Some(buf.toString))
        }
        case statement =>  println(statement); result += statement;
      }
      i = i + 1
    }
    result
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
        "route", "to"
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

    def statement: Parser[Statement] = created_by | set | route_to

    def created_by: Parser[CreatedBy] = "created" ~> "by" ~> ident ^^ {
      case user => CreatedBy(user)
    }

    def set: Parser[Set] = "set" ~> ident ~ opt(stringLit) ^^ {
      case key ~ value => Set(key, value)
    }

    def route_to: Parser[RouteTo] = "route" ~> "to" ~> ident ^^ {
      case user => RouteTo(user)
    }
  }
}


trait Statement
case class Invalid(message: String) extends Statement
case class Set(key2: String, value: Option[String]) extends Statement
case class CreatedBy(user: String) extends Statement
case class RouteTo(user: String) extends Statement
