package com.landonkuhn.proseeo.document

import org.apache.commons.lang.StringUtils._
import collection.Iterator

class Document(map: collection.mutable.Map[String, String] = new collection.mutable.HashMap[String, String]) extends collection.mutable.Map[String, String] {

	def get(key: String): Option[String] = map.get(key)

	def iterator: Iterator[(String, String)] = map.iterator

	def +=(kv: (String, String)): this.type = {
		map += kv
		this
	}

	def -=(key: String): this.type = {
		map -= key
		this
	}

	def scope(scope: String) = new ScopeDocument(this, scope)
	val scope: String = ""

	def view(f: (String, String) => Boolean) = new ViewDocument(this, f)

	def replace(map: Map[String, String]): Unit = {
		filter(a => ! map.contains(a._1)).toList.sortBy(_._1).foreach(a => this -= a._1)
		map.toList.sortBy(_._1).foreach(a => this += a._1 -> a._2)
	}

	def replace(document: Document): Unit = replace(document.toMap)

	override def toString = toStringImpl("")

	private def toStringImpl(pad: String): String = {
		val scopes = scala.collection.mutable.HashSet[String]()
		("" /: keys.toList.sorted)({ (s, key) =>
			s + (key.indexOf(".") match {
				case -1 => "%s %s\n".format(rightPad("%s%s ".format(pad, key), 100, "."), apply(key))
				case i => {
					val nextkey = key.substring(0, i)
					if (! scopes.contains(nextkey)) {
						scopes += nextkey
						"%s%s\n".format(pad, nextkey) + scope(nextkey + ".").toStringImpl(pad + "  ")
					} else ""
				}
			})
		})
	}

	val tree = new Tree(this)
}

object Document {
	def apply(): Document = apply(Map[String, String]())
	def apply(map: Map[String, String]): Document = {
		val _map = new scala.collection.mutable.HashMap[String, String]
		_map ++= map
		new Document(_map)
	}
	def apply(document: Document): Document = apply(document.toMap)
	def apply(values: (String, String)*): Document = apply(values.toMap)
	def fromJavaMap(map: java.util.Map[String, String]): Document = collection.JavaConversions.mapAsScalaMap(map) match {
		case document: Document => document
		case map: collection.mutable.Map[String, String] => new Document(map)
	}

	implicit def map_to_document(map: Map[String, String]): Document = Document(map)
	implicit def document_to_map(document: Document): Map[String, String] = document.toMap
}

class ScopeDocument[T <: Document](_parent: T, _scope: String) extends Document {
	override def get(key: String) = parent.get(_scope + key)
	override def iterator: Iterator[(String, String)] = parent.filter(_._1.startsWith(_scope)).map({ case (k, v) => k.substring(scopeLength) -> v }).iterator
	override def +=(kv: (String, String)): this.type = {
		parent += (_scope + kv._1) -> kv._2
		this
	}
	override def -=(key: String): this.type = {
		parent -= (_scope + key)
		this
	}
	val parent: T = _parent
	override val scope: String = parent.scope + _scope
	private val scopeLength = _scope.length
}

class ViewDocument[T <: Document](val parent: T, val f: (String, String) => Boolean) extends Document {
	override def get(key: String) = parent.get(key) match {
		case Some(value) if (f(key, value)) => Some(value)
		case _ => None
	}
	override def iterator: Iterator[(String, String)] = parent.filter(kv => f(kv._1, kv._2)).iterator
	override def +=(kv: (String, String)): this.type = {
		if (! f(kv._1, kv._2)) throw new IllegalArgumentException("Key/value [%s -> %s] violates the view predicate".format(kv._1, kv._2))
		parent += kv
		this
	}
	override def -=(key: String): this.type = {
		get(key) match {
			case Some(value) => parent -= key
			case None => {}
		}
		this
	}
}