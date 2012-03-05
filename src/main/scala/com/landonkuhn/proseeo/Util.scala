package com.landonkuhn.proseeo

import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTimeZone, DateTime}
import java.io.File
import org.apache.commons.io.FileUtils

object Util {

	implicit def rich_string(s: String) = new {
		def indent:String = indent("  ")
		def indent(chars: String):String = s.split("\n").map(chars + _).mkString("\n")
		def optLong:Option[Long] = try { Some(s.toLong) } catch { case _ => None }
		def isDate:Boolean = try { new DateTime(s); true } catch { case _ => false }
		def toDate:Date = (new DateTime(s)).toDate
		def optDate:Option[Date] = try { Some(toDate) } catch { case _ => None }
	}

	implicit def rich_date(d: Date) = new {
		def format: String = ISODateTimeFormat.dateTime.print(new DateTime(d, DateTimeZone.UTC))

		def diff(d2: Date): String = {
			val interval = (d.getTime - d2.getTime).abs
			if (interval < 60000) "just now"
			else if (interval < 60000 * 60) "%d minute(s)".format(interval / 60000)
			else if (interval < 60000 * 60 * 24) "%d hour(s)".format(interval / (60000 * 60))
			else "%d day(s)".format(interval / (60000 * 60 * 24))
		}
	}

	def now:Date = new Date

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
}