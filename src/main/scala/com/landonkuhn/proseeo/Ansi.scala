package com.landonkuhn.proseeo

object Ansi {
  implicit def to_ansi_string(s: String) = new {
    def bold = ansi(Console.BOLD, s)
    def underscore = ansi(Console.UNDERLINED, s)
    def blink = ansi(Console.BLINK, s)
    def reverse = ansi(Console.REVERSED, s)
    def conceal = ansi(Console.INVISIBLE, s)

    def black = ansi(Console.BLACK, s)
    def red = ansi(Console.RED, s)
    def green = ansi(Console.GREEN, s)
    def yellow = ansi(Console.YELLOW, s)
    def blue = ansi(Console.BLUE, s)
    def magenta = ansi(Console.MAGENTA, s)
    def cyan = ansi(Console.CYAN, s)
    def white = ansi(Console.WHITE, s)

    def bgblack = ansi(Console.BLACK_B, s)
    def bgred = ansi(Console.RED_B, s)
    def bggreen = ansi(Console.GREEN_B, s)
    def bgyellow = ansi(Console.YELLOW_B, s)
    def bgblue = ansi(Console.BLUE_B, s)
    def bgmagenta = ansi(Console.MAGENTA_B, s)
    def bgcyan = ansi(Console.CYAN_B, s)
    def bgwhite = ansi(Console.WHITE_B, s)
  }

  private def ansi(code: String, s: String) = {
    if (System.getenv("TERM") != null) {
      "%s%s%s".format(code, s, Console.RESET)
    } else s
  }
}