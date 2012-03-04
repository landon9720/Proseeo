package com.landonkuhn.proseeo

object Implicits {
  implicit def rich_string(s: String) = new {
    def tab(tab: String): String = Util.tab(s, tab)

    def optLong: Option[Long] = try { Some(s.toLong) } catch { case _ => None }
  }
}
