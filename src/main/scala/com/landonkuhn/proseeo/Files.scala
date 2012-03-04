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
    debug("File [%s] does not exist".format(file))
    None
  }

  private def lines(file: File): Seq[String] = {
    import collection.JavaConversions._
    val lines = readLines(file).toSeq
    debug("Read %d line(s) from file [%s] (file size %s)".format(lines.size, file, byteCountToDisplaySize(sizeOf(file))))
    lines
  }
  
  def write(fileName: String, lines: Seq[String]) { write(new File(fileName), lines) }
  def write(file: File, lines: Seq[String]) {
    import collection.JavaConversions._
    writeLines(file, lines)
    debug("Wrote %d line(s) to file [%s] (file size %s)".format(lines.size, file, byteCountToDisplaySize(sizeOf(file))))
  }

  def append(fileName: String, lines: Seq[String]) { write(new File(fileName), lines) }
  def append(file: File, lines: Seq[String]) {
    import collection.JavaConversions._
    writeLines(file, lines)
    debug("Appended %d line(s) to file [%s] (file size %s)".format(lines.size, file, byteCountToDisplaySize(sizeOf(file))))
  }
}
