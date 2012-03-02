package com.landonkuhn.proseeo.main

import com.landonkuhn.proseeo.document.Document

object Play {
  def play(script: Seq[Statement]): ScriptState = {
    val document = new Document

    for (statement <- script) statement match {
      case Set(key, Some(value)) => document += key -> value
      case x => println("I don't know about: " + x)
    }

    ScriptState(document)
  }
}

case class ScriptState(document: Document)