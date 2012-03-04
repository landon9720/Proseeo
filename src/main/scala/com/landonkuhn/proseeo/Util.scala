package com.landonkuhn.proseeo

import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import java.io.File
import org.apache.commons.io.FileUtils

object Util {
  def formatDateTime(date: Date): String = ISODateTimeFormat.dateTime.print(new DateTime(date, DateTimeZone.UTC))
  def parseDateTime(s: String): Date = (new DateTime(s)).toDate

  def editor(): Option[String] = {
    val f = File.createTempFile("proceeo_", ".value")
    f.deleteOnExit

    //    val out = new BufferedWriter(new FileWriter(f))
    //    out.write(s)
    //    out.close
    //
    //    debug("Launching editor: %s".format(f.getAbsolutePath))

    val args = System.getenv("EDITOR").split(" ").toList :+ f.getAbsolutePath
    val pb = new ProcessBuilder(args.toArray: _*)
    pb.start.waitFor

    import collection.JavaConversions._
    Some(FileUtils.readLines(f).mkString("\n"))

    //    val in = Source.fromFile(f)
    //    val result = in.getLines.mkString("\n")
    //    in.close
    //    result
  }

  def id: String = java.util.UUID.randomUUID.toString.replace("-", "")

  def indent(s: String, chars: String): String = s.split("\n").map(chars + _).mkString("\n")
}