package com.landonkuhn.proseeo.access

import com.landonkuhn.proseeo.Logging._
import com.landonkuhn.proseeo.Script
import java.io.{FileFilter, File}

case class Story(dir:File, name:String, script:Script)

class Stories(project:Project) {
	def all:Seq[Story] = for (dir <- (project.dir.listFiles(new FileFilter {
		def accept(file: File) = file.isDirectory && !file.getName.endsWith(".proseeo")
	})).view) yield get(dir.getName)

	def get(storyName:String) = Story(storyDir(storyName), storyName, new Script(scriptFile(storyName)))

	def test(storyName:String) = try { get(storyName); true } catch { case _:Dying => false }

	def scriptFile(storyName:String) = {
		val f = new File(storyDir(storyName), "script.proseeo")
		if (!f.canRead || !f.isFile) die("script file %s is not a readable file".format(f))
		f
	}

	def storyDir(storyName:String) = {
		val f = new File(project.dir, storyName)
		if (!f.canRead || !f.isDirectory) die("story directory %s is not a readable directory".format(f))
		f
	}
}