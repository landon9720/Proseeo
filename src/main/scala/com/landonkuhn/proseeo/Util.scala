package com.landonkuhn.proseeo

import java.util.Date

import org.joda.time.format.ISODateTimeFormat
import java.io.File
import org.apache.commons.io.FileUtils
import org.joda.time.{Duration, DateTimeZone, DateTime}
import com.eaio.util.text.HumanTime
import Files._
import Logging._

object Util {

	implicit def rich_string(s:String) = new {
		def indent:String = indent("  ")
		def indent(chars: String):String = s.split("\n").map(chars + _).mkString("\n")
		def optLong:Option[Long] = try { Some(s.toLong) } catch { case _ => None }
		def isDate:Boolean = try { new DateTime(s); true } catch { case _ => false }
		def toDate:Date = (new DateTime(s)).toDate
		def optDate:Option[Date] = try { Some(toDate) } catch { case _ => None }
	}

	implicit def rich_date(d:Date) = new {
		def format:String = ISODateTimeFormat.dateTime.print(new DateTime(d, DateTimeZone.UTC))
		def when(d0:Date):String =  if (d.getTime < d0.getTime) HumanTime.approximately(d.getTime - d0.getTime) + " ago"
																	else "in " + HumanTime.approximately(d.getTime - d0.getTime)
	}

	def now:Date = new Date

	def editor(file:File) {
//		val f = File.createTempFile(prefix + "_", ".edit.proseeo")
//		f.deleteOnExit
//		write(f, input)
		
//		val args = System.getenv("EDITOR").split(" ").toList :+ f.getAbsolutePath
//		val pb = new ProcessBuilder(args.toArray: _*)
//		pb.start.waitFor
		
//		scala.sys.process.Process(args) !
		
//		Runtime.getRuntime.exec(args.toArray).waitFor
		
//		File file = new File("FinalReport.doc");
		java.awt.Desktop.getDesktop.edit(file)
		
//		val result = read(f)
//		if (result.isEmpty) {
//			i("Forgotten")
//			None
//		} else Some(result)
	}

  def id: String = java.util.UUID.randomUUID.toString.replace("-", "")
}