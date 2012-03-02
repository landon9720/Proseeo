package com.landonkuhn.proseeo.document

import collection.Iterator

sealed class Tree(val document: Document = Document()) extends scala.collection.mutable.Map[String, Either[String, Tree]] {

  private val cache = new collection.mutable.HashMap[String, Tree]

	def get(key: String): Option[Either[String, Tree]] = (leaf(key), subtree(key)) match {
		case (Some(_), Some(_)) => sys.error("Document key [%s] maps to both value and tree in document:\n%s".format(key, toString))
		case (Some(value), None) => Some(Left(value))
		case (None, Some(tree)) => Some(Right(tree))
		case (None, None) => None
	}

	def iterator: Iterator[(String, Either[String, Tree])] = new Iterator[(String, Either[String, Tree])] {
		val keyIterator = ((for (key <- document.keys) yield {
			val i = key.indexOf(".")
			if (i == -1) key else key.substring(0, i)
		}).toSet | cache.keySet).iterator
		def hasNext: Boolean = keyIterator.hasNext
		def next: (String, Either[String, Tree]) = {
			val key = keyIterator.next
			key -> apply(key)
		}
	}

  def +=(kv: (String, Either[String, Tree])): this.type = {
		kv match {
			case (k, Left(v)) => leaf(k, v)
			case (k, Right(v)) => subtree(k, v)
		}
		this
	}

	def -=(key: String): this.type = delete(key)

	def leaf(key: String): Option[String] = document.get(key)

	def leaf(key: String, value: String): this.type = {
		delete(key)
		document.put(key, value)
		this
	}

	def subtree(key: String): Option[Tree] = cache.get(key) match {
		case Some(tree) => Some(tree)
		case _ => document.scope(key + ".") match {
			case document: Document if document.size == 0 => None
			case document: Document => {
				val tree = document.tree
				cache += key -> tree
				Some(tree)
			}
		}
	}

	def subtree(key: String, tree: Tree): this.type = {
		// the incoming tree needs to be COPIED NODE-BY-NODE in order to preserve the source tree's cache
		delete(key)
		val destination = document.scope(key + ".").tree
		for (node <- tree) destination += node
		cache += key -> destination
		this
	}

	def delete(key: String): this.type = {
		document -= key
		document.scope(key + ".").clear
		cache -= key
		this
	}

	def leafs: Map[String, String] = collect({ case (key, Left(leaf)) => key -> leaf }).toMap

	def subtrees: Map[String, Tree] = collect({ case (key, Right(subtree)) => key -> subtree }).toMap

	override def toString: String = document.toString

	def replace(tree: Tree): this.type = {
		clear
		this ++= tree
	}
}

object Tree {

}