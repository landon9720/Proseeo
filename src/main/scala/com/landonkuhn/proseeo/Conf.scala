package com.landonkuhn.proseeo

import java.io.File

import Logging._
import Files._
import collection.Iterator
import org.apache.commons.lang3.StringUtils
import StringUtils._
import collection.mutable.{ListBuffer, Map, HashMap}

class Conf(val file: File, val parent: Option[Conf] = None) extends Map[String, String] {

  def iterator: Iterator[(String, String)] = conf.iterator
  def get(key: String): Option[String] = conf.get(key)
  def +=(kv: (String, String)): this.type = {
    conf += kv
    this
  }
  def -=(key: String): this.type = {
    conf -= key
    this
  }
  
  def required(key: String): String = getOrElse(key, {
    error("Missing required configuration key [%s] from files:\n%s".format(key, files.mkString("\n  ")))
    sys.exit
  })

  def save {
    write(file, toStrings)
  }

  private def toStrings: Seq[String] = (for ((k, v) <- this) yield "%s: %s".format(k, v)).toSeq
  override def toString: String = toStrings.mkString("\n")

  private val conf: Map[String, String] = {
    val conf = new HashMap[String, String]
    for (line <- read(file).map(trim) if line.length >0 && ! startsWith(line, "#")) {
      val kv = split(line, "=:", 2)
      if (kv.length != 2) {
        error("Invalid line in configuration file [%s]:")
        error(line)
        sys.exit
      }
      conf += trim(kv(0)) -> trim(kv(1))
    }
    debug("Read %d value(s) from configuration file [%s]".format(conf.size, file))
    conf
  }
  
  private val files: Seq[File] = {
    val result = new ListBuffer[File]
    var conf: Option[Conf] = Some(this)
    while (conf.isDefined) {
      result += conf.get.file
      conf = conf.get.parent
    }
    result
  }
}