package com.landonkuhn.proseeo

import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import java.io._
import org.joda.time.{DateTimeZone, DateTime}
import com.ocpsoft.pretty.time.PrettyTime
import com.eaio.util.text.HumanTime

object Util {

	implicit def rich_string(s:String) = new {
		def indent:String = indent("  ")
		def indent(chars: String):String = s.split("\n").map(chars + _).mkString("\n")
		def optLong:Option[Long] = try { Some(s.toLong) } catch { case _ => None }
		def isDate:Boolean = try { new DateTime(s); true } catch { case _ => false }
		def toDate:Date = (new DateTime(s)).toDate
		def optDate:Option[Date] = try { Some(toDate) } catch { case _ => None }
		def toDuration:Long = HumanTime.eval(s).getDelta
	}

	implicit def rich_date(d:Date) = new {
		def format:String = ISODateTimeFormat.dateTime.print(new DateTime(d, DateTimeZone.UTC))
		def when(d0:Date):String = new PrettyTime(d0).format(d)
		def when:String = when(now)
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