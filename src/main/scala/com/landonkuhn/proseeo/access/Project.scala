package com.landonkuhn.proseeo.access

import java.io.File
import com.landonkuhn.proseeo.Logging._
import com.landonkuhn.proseeo.{Document, Conf}
import org.apache.commons.lang3.StringUtils
import StringUtils._

case class Project(dir:File, file:File, conf:Conf, name:String, id:String) {
	val users = (for ((userName, user) <- new Document(conf).scope("project.users.").tree.subtrees) yield {
		userName -> User(userName, user.leaf("name"), user.leaf("email"))
	}).toMap
	val groups = (for ((group, members) <- new Document(conf).scope("project.groups.").tree.leafs) yield {
		group -> Group(group, members.split(",").map(trim).map(users(_)).toSet)
	}).toMap
}

case class User(userName:String, fullName:Option[String], email:Option[String])
case class Group(groupName:String, members:Set[User])

object Project {
	def test(projectDir:File) = {
		lazy val projectFile = new File(projectDir, "project.proseeo")
		projectDir.canRead && projectDir.isDirectory && projectFile.canRead && projectFile.isFile
	}

	def get(projectDir:File):Project = {
		if (!test(projectDir)) die("there is no project here")
		val projectFile = new File(projectDir, "project.proseeo")
		val conf = new Conf(projectFile)
		Project(projectDir, projectFile, conf, conf.required("project.name"), conf.required("project.id"))
	}
}
