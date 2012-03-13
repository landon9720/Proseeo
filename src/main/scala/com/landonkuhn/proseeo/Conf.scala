package com.landonkuhn.proseeo

import java.io.File

import Logging._
import Files._
import collection.Iterator
import org.apache.commons.lang3.StringUtils
import StringUtils._
import collection.mutable.{ListBuffer, Map, HashMap}

import Util._

class Conf(file: File) extends Map[String, String] {

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
  
  def required(key: String): String = getOrElse(key, die("I can't find the configuration value for [%s]".format(key)))

  def save {
    write(file, toStrings)
  }

  private def toStrings: Seq[String] = {
    val width = if (keys.isEmpty) 0 else keys.map(_.length).max
		(for (k <- keys.toSeq.sorted) yield "%s : %s".format(rightPad(k, width), apply(k))).toSeq
  }
  override def toString: String = toStrings.mkString("\n")

  private val conf: Map[String, String] = {
    val conf = new HashMap[String, String]
    for (line <- readIfExists(file).getOrElse(List()).map(trim) if line.length > 0 && ! startsWith(line, "#")) {
      val kv = split(line, "=:", 2)
      if (kv.length != 2) die("Invalid line in configuration file [%s]: %s".format(line))
      conf += trim(kv(0)) -> trim(kv(1))
    }
    conf
  }
}