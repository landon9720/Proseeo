package com.landonkuhn.proseeo.template

import org.junit._
import Assert._

import com.landonkuhn.proseeo
import proseeo.Logging
import Logging._

import proseeo.plan.PlanLineParser
import PlanLineParser._

class PlanLineParserTest {

	doNotDie = true

	@Test
	def test {
		val template = """
need title:text
need description:text
want version_reported_in:enum(versions)

need scrubbed:gate

need version_assignment:enum(versions)

need fixed_in_version:enum(versions)
need release_note:text
need fixed:gate

need test_note:text
need tested:gate
"""
//		val result = (for (line <- template.split("\n")) yield {
//			""""%s"\n\t%s\n""".format(line, parseLine(line))
//		}).mkString("\n")
//		assertEquals("""
//""", result)
	}
}
