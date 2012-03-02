package com.landonkuhn.proseeo.play

import com.landonkuhn.proseeo.document.Document
import collection.mutable.ListBuffer
import com.landonkuhn.proseeo.script._

object Play {
  def play(script: Seq[Statement]): ScriptState = {
    val document = new Document

    val history = new ListBuffer[String]
    var current: Option[RouteTo] = None

    val comments = new ListBuffer[String]

    for (statement <- script) statement match {
      case c @ CreatedBy(user) => {
        
      }
      case set @ Set(key, _) => document += key -> set.value
      case routeTo @ RouteTo(user, next) => {
        history += user
        current = Some(routeTo)
      }
      case say: Say => comments += say.value
      case x => println("I don't know about: " + x)
    }

    ScriptState(document, history, current, comments)
  }
}

case class ScriptState(document: Document, stack: Seq[String], current: Option[RouteTo], comments: Seq[String])
