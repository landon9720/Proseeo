package com.landonkuhn.proseeo

import org.junit._
import org.junit.Assert._

import Util._

class UtilTest {
	@Test def justnow1 { assertEquals("just now", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T00:00:00Z".toDate)) }
	@Test def justnow2 { assertEquals("just now", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T00:00:01Z".toDate)) }
	@Test def minutes1 { assertEquals("1 minute(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T00:01:00Z".toDate)) }
	@Test def minutes2 { assertEquals("2 minute(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T00:02:00Z".toDate)) }
	@Test def hours1 { assertEquals("1 hour(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T01:00:00Z".toDate)) }
	@Test def hours2 { assertEquals("2 hour(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-01T02:00:00Z".toDate)) }
	@Test def days1 { assertEquals("1 day(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-02T00:00:00Z".toDate)) }
	@Test def days2 { assertEquals("2 day(s)", "2012-01-01T00:00:00Z".toDate.diff("2012-01-03T00:00:00Z".toDate)) }
}