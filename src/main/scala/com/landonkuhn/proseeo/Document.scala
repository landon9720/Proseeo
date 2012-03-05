package com.landonkuhn.proseeo

import org.apache.commons.lang3.StringUtils._
import collection.Iterator

class Document(map:collection.mutable.Map[String, String] = new collection.mutable.HashMap[String, String]) extends collection.mutable.Map[String, String] {

	def get(key:String):Option[String] = map.get(key)

	def iterator:Iterator[(String, String)] = map.iterator

	def +=(kv:(String, String)):this.type = {
		map += kv
		this
	}

	def -=(key:String):this.type = {
		map -= key
		this
	}

	def scope(scope:String) = new ScopeDocument(this, scope)

	val scope:String = ""

	def view(f:(String, String) => Boolean) = new ViewDocument(this, f)

	def replace(map:Map[String, String]):Unit = {
		filter(a => !map.contains(a._1)).toList.sortBy(_._1).foreach(a => this -= a._1)
		map.toList.sortBy(_._1).foreach(a => this += a._1 -> a._2)
	}

	def replace(document:Document):Unit = replace(document.toMap)

	override def toString = toStringImpl("")

	private def toStringImpl(pad:String):String = {
		val scopes = scala.collection.mutable.HashSet[String]()
		("" /: keys.toList.sorted)({
			(s, key) =>
				s + (key.indexOf(".") match {
					case -1 => "%s %s\n".format(rightPad("%s%s ".format(pad, key), 100, "."), apply(key))
					case i => {
						val nextkey = key.substring(0, i)
						if (!scopes.contains(nextkey)) {
							scopes += nextkey
							"%s%s\n".format(pad, nextkey) + scope(nextkey + ".").toStringImpl(pad + "  ")
						} else ""
					}
				})
		})
	}

	val tree = new Tree(this)
}

class ScopeDocument[T <: Document](_parent:T, _scope:String) extends Document {
	override def get(key:String) = parent.get(_scope + key)

	override def iterator:Iterator[(String, String)] = parent.filter(_._1.startsWith(_scope)).map({
		case (k, v) => k.substring(scopeLength) -> v
	}).iterator

	override def +=(kv:(String, String)):this.type = {
		parent += (_scope + kv._1) -> kv._2
		this
	}

	override def -=(key:String):this.type = {
		parent -= (_scope + key)
		this
	}

	val parent:T = _parent
	override val scope:String = parent.scope + _scope
	private val scopeLength = _scope.length
}

class ViewDocument[T <: Document](val parent:T, val f:(String, String) => Boolean) extends Document {
	override def get(key:String) = parent.get(key) match {
		case Some(value) if (f(key, value)) => Some(value)
		case _ => None
	}

	override def iterator:Iterator[(String, String)] = parent.filter(kv => f(kv._1, kv._2)).iterator

	override def +=(kv:(String, String)):this.type = {
		if (!f(kv._1, kv._2)) throw new IllegalArgumentException("Key/value [%s -> %s] violates the view predicate".format(kv._1, kv._2))
		parent += kv
		this
	}

	override def -=(key:String):this.type = {
		get(key) match {
			case Some(value) => parent -= key
			case None => {}
		}
		this
	}
}

sealed class Tree(val document:Document = new Document()) extends scala.collection.mutable.Map[String, Either[String, Tree]] {

	private val cache = new collection.mutable.HashMap[String, Tree]

	def get(key:String):Option[Either[String, Tree]] = (leaf(key), subtree(key)) match {
		case (Some(_), Some(_)) => sys.error("Document key [%s] maps to both value and tree in document:\n%s".format(key, toString))
		case (Some(value), None) => Some(Left(value))
		case (None, Some(tree)) => Some(Right(tree))
		case (None, None) => None
	}

	def iterator:Iterator[(String, Either[String, Tree])] = new Iterator[(String, Either[String, Tree])] {
		val keyIterator = ((for (key <- document.keys) yield {
			val i = key.indexOf(".")
			if (i == -1) key else key.substring(0, i)
		}).toSet | cache.keySet).iterator

		def hasNext:Boolean = keyIterator.hasNext

		def next:(String, Either[String, Tree]) = {
			val key = keyIterator.next
			key -> apply(key)
		}
	}

	def +=(kv:(String, Either[String, Tree])):this.type = {
		kv match {
			case (k, Left(v)) => leaf(k, v)
			case (k, Right(v)) => subtree(k, v)
		}
		this
	}

	def -=(key:String):this.type = delete(key)

	def leaf(key:String):Option[String] = document.get(key)

	def leaf(key:String, value:String):this.type = {
		delete(key)
		document.put(key, value)
		this
	}

	def subtree(key:String):Option[Tree] = cache.get(key) match {
		case Some(tree) => Some(tree)
		case _ => document.scope(key + ".") match {
			case document:Document if document.size == 0 => None
			case document:Document => {
				val tree = document.tree
				cache += key -> tree
				Some(tree)
			}
		}
	}

	def subtree(key:String, tree:Tree):this.type = {
		// the incoming tree needs to be COPIED NODE-BY-NODE in order to preserve the source tree's cache
		delete(key)
		val destination = document.scope(key + ".").tree
		for (node <- tree) destination += node
		cache += key -> destination
		this
	}

	def delete(key:String):this.type = {
		document -= key
		document.scope(key + ".").clear
		cache -= key
		this
	}

	def leafs:Map[String, String] = collect({
		case (key, Left(leaf)) => key -> leaf
	}).toMap

	def subtrees:Map[String, Tree] = collect({
		case (key, Right(subtree)) => key -> subtree
	}).toMap

	override def toString:String = document.toString

	def replace(tree:Tree):this.type = {
		clear
		this ++= tree
	}
}