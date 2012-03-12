package com.landonkuhn.proseeo.access

import com.landonkuhn.proseeo.Logging._
import com.landonkuhn.proseeo.scriptmodel.Script
import java.io.{FileFilter, File}

case class Story(name:String, script:Script)

class Stories(projectDir:File) {
	def all:Seq[Story] = for (dir <- (projectDir.listFiles(new FileFilter {
		def accept(file: File) = file.isDirectory && !file.getName.endsWith(".proseeo")
	})).view) yield get(dir.getName)

	def get(storyName:String) = Story(storyName, new Script(scriptFile(storyName)))

	def storyDir(storyName:String) = {
		val f = new File(projectDir, storyName)
		if (!f.canRead || !f.isDirectory) die("story directory %s is not a readable directory".format(f))
		f
	}

	def scriptFile(storyName:String) = {
		val f = new File(storyDir(storyName), "script.proseeo")
		if (!f.canRead || !f.isFile) die("script file %s is not a readable file".format(f))
		f
	}
}