package com.landonkuhn.proseeo.main

import com.landonkuhn.proseeo.document.Document
import collection.mutable.ListBuffer

object Play {
  def play(script: Seq[Statement]): ScriptState = {
    val document = new Document

    val history = new ListBuffer[String]
    var current: Option[RouteTo] = None

    for (statement <- script) statement match {
//      case c @ CreatedBy(user) => {
//        if (route.isDefined) sys.error("invalid created by: " + c)
//        route = Some(Route(user))
//      }
      case routeTo @ RouteTo(user, next) => {
        println("history +- " + user)
        history += user
        current = Some(routeTo)
      }
      case Set(key, Some(value)) => document += key -> value
      case x => println("I don't know about: " + x)
    }

    ScriptState(document, history, current)
  }
}

case class ScriptState(document: Document, stack: Seq[String], current: Option[RouteTo])
