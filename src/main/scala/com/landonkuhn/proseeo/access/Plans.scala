package com.landonkuhn.proseeo.access

import java.io.File
import com.landonkuhn.proseeo.{Plan, Logging}
import Logging._

class Plans(projectDir:File, storyDir:File) {

	def get = new Plan(planFile(storyDir))

	private def planFile(storyDir:File):File = {
		val f = new File(storyDir, "plan.proseeo")
		if (!f.canRead || !f.isFile) die("plan file %s is not a readable file".format(f))
		f
	}

	def projectPlanFile(planName:String):File = new File(projectDir, "%s.plan.proseeo".format(planName))

	def testProjectPlanFile(planName:String):Boolean = {
		val f = projectPlanFile(planName)
		f.canRead && f.isFile
	}
}
