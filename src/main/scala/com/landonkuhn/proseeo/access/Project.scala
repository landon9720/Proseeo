package com.landonkuhn.proseeo.access

import java.io.File
import com.landonkuhn.proseeo.Logging._
import com.landonkuhn.proseeo.Conf

case class Project(projectDir:File, conf:Conf, name:String, id:String)

object Project {
	def test(projectDir:File) = {
		val projectFile = new File(projectDir, "project.proseeo")
		projectFile.canRead && projectFile.isFile
	}

	def get(projectDir:File):Project = {
		if (!projectDir.canRead || !projectDir.isDirectory) die("story directory %s is not a readable directory".format(projectDir))
		if (!test(projectDir)) die("project file is not a readable file")
		val conf = new Conf(new File(projectDir, "project.proseeo"))
		Project(projectDir, conf, conf.required("project.name"), conf.required("project.id"))
	}
}
