package com.landonkuhn.proseeo

import java.util.Date

import Logging._
import Util._

class DocumentStory(storyDocument:Document) extends Story {
	val name:String = storyDocument.get("name").getOrElse(die("indexed story is missing name"))
	val plan:Option[String] = storyDocument.get("plan")
	val created:Option[Created] = storyDocument.tree.subtree("created") map {
		created => Created(created.leaf("by").get, created.leaf("at").get.toDate)
	}
	val ended:Option[Ended] = storyDocument.tree.subtree("ended") map {
		ended => Ended(ended.leaf("by").get, ended.leaf("at").get.toDate)
	}
	val tail:Option[Statement] = storyDocument.tree.subtree("tail") map {
		tail => new Statement {
			def by:String = tail.leaf("by").get
			def at:Date = tail.leaf("at").get.toDate
		}
	}
	val document:Document = storyDocument.scope("document.")
	val route:RouteState = RouteState(
		past = storyDocument.get("route.past").toSeq, // later multivalues
		present = storyDocument.get("route.present"),
		future = storyDocument.get("route.future").toSeq // later
	)
}
