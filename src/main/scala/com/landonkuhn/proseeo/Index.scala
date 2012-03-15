package com.landonkuhn.proseeo

import access.{Stories, Project}
import Util._
import org.apache.lucene.store.FSDirectory
import java.io.File
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.document.{Document=>D, Field=>F}
import org.apache.lucene.index.{IndexReader, IndexWriter, IndexWriterConfig}
import org.apache.lucene.search.{MatchAllDocsQuery, Collector, IndexSearcher}

class LuceneSupport {
	val dir = FSDirectory.open(new File("index.proseeo"))
	val analyzer = new StandardAnalyzer(Version.LUCENE_35)
}

class Index(project:Project) extends LuceneSupport {
	val iwc = new IndexWriterConfig(Version.LUCENE_35, analyzer)
	iwc.setOpenMode(OpenMode.CREATE)
	val writer = new IndexWriter(dir, iwc)

	val stories = new Stories(project)
	for (story <- stories.all) {
		val state = story.script.state
		writer.addDocument {
			val d = new D
			d.add(
				new F("story.name", story.name, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
			for (created <- state.created) {
				d.add(new F("story.script.created.at", created.at.format, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
				d.add(new F("story.script.created.by", created.by, F.Store.YES, F.Index.NOT_ANALYZED_NO_NORMS, F.TermVector.NO))
			}
			println(d.toString)
			d
		}
	}

	writer.close()
}

class Report(project:Project) extends LuceneSupport {
	val reader = IndexReader.open(dir, true)
	val searcher = new IndexSearcher(reader)
	val query = new MatchAllDocsQuery
	val docs = searcher.search(query, 100)
	println("totalHits " + docs.totalHits)
	for (doc <- docs.scoreDocs) {
		println(doc.score)
		import collection.JavaConversions._
		val map = (Map[String, String]() /: reader.document(doc.doc).getFields) { (map, f) =>
			map + (f.name -> f.stringValue)
		}
		println(new Document(map).toString)
	}
	searcher.close
	reader.close
}