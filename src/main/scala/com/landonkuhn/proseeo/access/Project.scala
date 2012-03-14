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
	def actor(actorName:String):Option[Either[User, Group]] = users.get(actorName) match {
		case Some(user) => Some(Left(user))
		case _ => groups.get(actorName) match {
			case Some(group) => Some(Right(group))
			case _ => None
		}
	}
	def isUser(actorName:String) = actor(actorName).map(_.isLeft).getOrElse(false)
	def isGroup(actorName:String) = actor(actorName).map(_.isRight).getOrElse(false)
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
