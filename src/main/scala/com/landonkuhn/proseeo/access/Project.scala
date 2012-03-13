package com.landonkuhn.proseeo.access

import java.io.File
import com.landonkuhn.proseeo.Logging._
import com.landonkuhn.proseeo.Conf

case class Project(projectDir:File, conf:Conf, name:String, id:String)

object Project {
	def test(projectDir:File) = {
		lazy val projectFile = new File(projectDir, "project.proseeo")
		projectDir.canRead && projectDir.isDirectory && projectFile.canRead && projectFile.isFile
	}

	def get(projectDir:File):Project = {
		if (!test(projectDir)) die("there is no project here")
		val conf = new Conf(new File(projectDir, "project.proseeo"))
		Project(projectDir, conf, conf.required("project.name"), conf.required("project.id"))
	}
}
