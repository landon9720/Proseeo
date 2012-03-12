package com.landonkuhn.proseeo.index

import org.apache.lucene.store.FSDirectory
import java.io.File
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriterConfig.OpenMode
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}

object Index {

	val dir = FSDirectory.open(new File("index.proseeo"))
	val analyzer = new StandardAnalyzer(Version.LUCENE_35)
	val iwc = new IndexWriterConfig(Version.LUCENE_35, analyzer)
	iwc.setOpenMode(OpenMode.CREATE_OR_APPEND)
	val writer = new IndexWriter(dir, iwc)
	
}