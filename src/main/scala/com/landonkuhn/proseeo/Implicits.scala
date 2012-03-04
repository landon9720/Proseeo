package com.landonkuhn.proseeo

object Implicits {
  implicit def rich_string(s: String) = new {
    def indent(chars: String): String = Util.indent(s, chars)
    def optLong: Option[Long] = try { Some(s.toLong) } catch { case _ => None }
  }
}