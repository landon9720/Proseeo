package com.landonkuhn.proseeo

import java.io.File
import collection.mutable.Map
import collection.mutable.HashMap

import Logging._
import Files._
import collection.Iterator
import org.apache.commons.lang3.StringUtils
import StringUtils._

class Conf(file: File, parent: Option[Conf] = None) extends Map[String, String] {

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

  private val conf: Map[String, String] = {
    val conf = new HashMap[String, String]
    for (line <- read(file).map(trim) if ! startsWith(line, "#")) {
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
}