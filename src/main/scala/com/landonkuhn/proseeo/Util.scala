package com.landonkuhn.proseeo

import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import java.io._
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils.copy
import org.joda.time.{Duration, DateTimeZone, DateTime}
import com.eaio.util.text.HumanTime
import Files._
import Logging._
import scala.sys.process._

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
		def when(d0:Date):String =  if (d.getTime < d0.getTime) HumanTime.exactly(d.getTime - d0.getTime) + " ago"
																	else "in " + HumanTime.exactly(d.getTime - d0.getTime)
	}

	implicit def rich_seq[T](s:Seq[T]) = new {
		def dedupe:Seq[T] = (List[T]() /: s) { (elements, element) =>
			if (elements.lastOption != Some(element)) elements :+ element
			else elements
		}
	}

	def now = new Date
	def id = java.util.UUID.randomUUID.toString.replace("-", "")
	val cwd = new File(".")
}