package com.landonkuhn.proseeo.document

import java.io.File
import org.apache.commons.io.FileUtils._
import org.apache.commons.lang3.StringUtils._
import collection.JavaConversions._

object DocumentIo {

  def read(file: File): Document = {
    val document = new Document
    for (lineElement <- lineIterator(file)) {
      val line = lineElement.toString
      val keyValue = split(line, "=:", 2)
      if (keyValue.length != 2) sys.error("Invalid line: " + line)
      document += trim(keyValue(0)) -> trim(keyValue(1))
    }
    document
  }

  def write(file: File, document: Document) {

  }
}