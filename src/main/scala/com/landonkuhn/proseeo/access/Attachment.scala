package com.landonkuhn.proseeo.access

import java.io.File
import org.apache.commons.io.FileUtils._

import com.landonkuhn.proseeo
import proseeo.Logging
import Logging._

case class Attachment(fileName:String, size:String)

object Attachment {
	def apply(dir:File):Seq[Attachment] = {
	  if (!dir.isDirectory) die("%s is not a directory".format(dir))
		val files = Option(dir.listFiles).getOrElse(die("could not get files in %s".format(dir)))
		for (file <- files if !file.getName.endsWith(".proseeo"))
			yield Attachment(new File(dir, file.getName).toString, byteCountToDisplaySize(file.length))
	}
}
