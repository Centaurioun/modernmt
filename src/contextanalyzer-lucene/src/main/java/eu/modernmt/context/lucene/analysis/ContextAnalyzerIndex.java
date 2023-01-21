package eu.modernmt.context.lucene.analysis;

import eu.modernmt.context.lucene.analysis.rescoring.CosineSimilarityRescorer;
import eu.modernmt.context.lucene.analysis.rescoring.Rescorer;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.model.corpus.Corpus;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/** Created by davide on 10/07/15. */
public class ContextAnalyzerIndex implements Closeable {

  private static final int MIN_RESULT_BATCH = 20;

  private final Directory indexDirectory;
  private final Analyzer analyzer;
  private final IndexWriter indexWriter;
  private final Rescorer rescorer;

  private DirectoryReader _indexReader;
  private IndexSearcher _indexSearcher;

  private static File forceMkdir(File directory) throws IOException {
    if (!directory.isDirectory()) FileUtils.forceMkdir(directory);
    return directory;
  }

  public ContextAnalyzerIndex(File indexPath) throws IOException {
    this(indexPath, new CosineSimilarityRescorer());
  }

  public ContextAnalyzerIndex(Directory directory) throws IOException {
    this(directory, new CosineSimilarityRescorer());
  }

  public ContextAnalyzerIndex(File indexPath, Rescorer rescorer) throws IOException {
    this(FSDirectory.open(forceMkdir(indexPath)), rescorer);
  }

  public ContextAnalyzerIndex(Directory directory, Rescorer rescorer) throws IOException {
    this.indexDirectory = directory;
    this.analyzer = new CorpusAnalyzer();
    this.rescorer = rescorer;

    // Index writer setup
    IndexWriterConfig indexConfig = new IndexWriterConfig(Version.LUCENE_4_10_4, this.analyzer);
    indexConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
    indexConfig.setSimilarity(
        new DefaultSimilarity() {

          @Override
          public float lengthNorm(FieldInvertState state) {
            return 1.f;
          }
        });

    this.indexWriter = new IndexWriter(this.indexDirectory, indexConfig);

    // Ensure index exists
    if (!DirectoryReader.indexExists(directory)) this.indexWriter.commit();
  }

  public synchronized IndexReader getIndexReader() throws IOException {
    if (this._indexReader == null) {
      this._indexReader = DirectoryReader.open(this.indexDirectory);
      this._indexReader.incRef();
      this._indexSearcher = new IndexSearcher(this._indexReader);
    } else {
      DirectoryReader reader = DirectoryReader.openIfChanged(this._indexReader);

      if (reader != null) {
        this._indexReader.close();
        this._indexReader = reader;
        this._indexReader.incRef();

        this._indexSearcher = new IndexSearcher(this._indexReader);
      }
    }

    return this._indexReader;
  }

  public IndexSearcher getIndexSearcher() throws IOException {
    getIndexReader();
    return this._indexSearcher;
  }

  public void update(Document document) throws IOException {
    String id = DocumentBuilder.getId(document);
    this.indexWriter.updateDocument(DocumentBuilder.makeIdTerm(id), document);
  }

  public void delete(long memory) throws IOException {
    Term memoryTerm = DocumentBuilder.makeMemoryTerm(memory);
    this.indexWriter.deleteDocuments(memoryTerm);
  }

  public void flush() throws IOException {
    this.indexWriter.commit();
  }

  public void clear() throws IOException {
    this.indexWriter.deleteAll();
    this.indexWriter.commit();
  }

  public void forceMerge() throws IOException {
    this.indexWriter.forceMerge(1);
    this.indexWriter.commit();
  }

  public ContextVector getContextVector(
      UUID user, LanguageDirection direction, Corpus queryDocument, int limit) throws IOException {
    return this.getContextVector(user, direction, queryDocument, limit, this.rescorer);
  }

  public ContextVector getContextVector(
      UUID user, LanguageDirection direction, Corpus queryDocument, int limit, Rescorer rescorer)
      throws IOException {
    String contentFieldName = DocumentBuilder.makeContentFieldName(direction);

    IndexSearcher searcher = this.getIndexSearcher();
    IndexReader reader = searcher.getIndexReader();

    // Get matching documents

    int rawLimit = limit < MIN_RESULT_BATCH ? MIN_RESULT_BATCH : limit;

    MoreLikeThis mlt = new MoreLikeThis(reader);
    mlt.setFieldNames(new String[] {contentFieldName});
    mlt.setMinDocFreq(0);
    mlt.setMinTermFreq(1);
    mlt.setMinWordLen(2);
    mlt.setBoost(true);
    mlt.setAnalyzer(analyzer);

    TopScoreDocCollector collector = TopScoreDocCollector.create(rawLimit, true);

    Reader queryDocumentReader = queryDocument.getRawContentReader();

    try {
      Query mltQuery = mlt.like(contentFieldName, queryDocumentReader);
      BooleanQuery ownerQuery = new BooleanQuery();

      if (user == null) {
        ownerQuery.add(DocumentBuilder.makePublicOwnerMatchingQuery(), BooleanClause.Occur.MUST);
      } else {
        ownerQuery.add(DocumentBuilder.makePublicOwnerMatchingQuery(), BooleanClause.Occur.SHOULD);
        ownerQuery.add(DocumentBuilder.makeOwnerMatchingQuery(user), BooleanClause.Occur.SHOULD);
        ownerQuery.setMinimumNumberShouldMatch(1);
      }

      FilteredQuery query = new FilteredQuery(mltQuery, new QueryWrapperFilter(ownerQuery));
      searcher.search(query, collector);
    } finally {
      IOUtils.closeQuietly(queryDocumentReader);
    }

    ScoreDoc[] topDocs = collector.topDocs().scoreDocs;

    // Rescore result

    if (rescorer != null) {
      Document referenceDocument = DocumentBuilder.newInstance(direction, queryDocument);
      rescorer.rescore(reader, this.analyzer, topDocs, referenceDocument, contentFieldName);
    }

    // Build result

    ContextVector.Builder resultBuilder = new ContextVector.Builder(topDocs.length);
    resultBuilder.setLimit(limit);

    for (ScoreDoc topDocRef : topDocs) {
      Document topDoc = searcher.doc(topDocRef.doc);

      long memory = DocumentBuilder.getMemory(topDoc);
      resultBuilder.add(memory, topDocRef.score);
    }

    return resultBuilder.build();
  }

  @Override
  public void close() {
    IOUtils.closeQuietly(this._indexReader);
    IOUtils.closeQuietly(this.indexWriter);
    IOUtils.closeQuietly(this.indexDirectory);
  }
}
