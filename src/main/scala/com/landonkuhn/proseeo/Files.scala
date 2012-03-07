package com.landonkuhn.proseeo

import java.io.File
import org.apache.commons.io.FileUtils._
import Logging._

object Files {

  def read(fileName: String): Seq[String] = read(new File(fileName))
  def read(file: File): Seq[String] = {
    if (! file.canRead) {
      error("Cannot read file [%s]".format(file))
      sys.exit
    }
    lines(file)
  }

  def readIfExists(fileName: String): Option[Seq[String]] = readIfExists(new File(fileName))
  def readIfExists(file: File): Option[Seq[String]] = if (file.exists) Some(read(file)) else {
    None
  }

  private def lines(file: File): Seq[String] = {
    import collection.JavaConversions._
    readLines(file).toSeq
  }
  
  def write(fileName: String, lines: Seq[String]) { write(new File(fileName), lines) }
  def write(file: File, lines: Seq[String]) {
    import collection.JavaConversions._
    writeLines(file, lines)
  }

  def append(fileName: String, lines: Seq[String]) { write(new File(fileName), lines) }
  def append(file: File, lines: Seq[String]) {
    import collection.JavaConversions._
    writeLines(file, lines)
  }
}
