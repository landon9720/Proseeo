package com.landonkuhn.proseeo

import java.io.File

import Logging._
import Files._
import collection.Iterator
import org.apache.commons.lang3.StringUtils
import StringUtils._
import collection.mutable.{ListBuffer, Map, HashMap}

import Implicits._

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
    die("I can't find the configuration value for [%s]".format(key), files.mkString("\n"))
  })

  def save {
    write(file, toStrings)
    debug("Saved configuration file [%s]:\n%s".format(file, toString.indent("  ")))
  }

  private def toStrings: Seq[String] = {
    val width = keys.map(_.length).max
		(for (k <- keys.toSeq.sorted) yield "%s : %s".format(rightPad(k, width, ' '), apply(k))).toSeq
  }
  override def toString: String = toStrings.mkString("\n")

  private val conf: Map[String, String] = {
    val conf = new HashMap[String, String]
    for (line <- readIfExists(file).getOrElse(List()).map(trim) if line.length > 0 && ! startsWith(line, "#")) {
      val kv = split(line, "=:", 2)
      if (kv.length != 2) {
        error("Invalid line in configuration file [%s]:")
        error(line)
        sys.exit
      }
      conf += trim(kv(0)) -> trim(kv(1))
    }
    conf
  }
	debug("Read configuration file [%s]:\n%s".format(file, toString.indent("  ")))
  
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