package eu.modernmt.decoder.neural.memory;

import static eu.modernmt.decoder.neural.memory.TestData.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import eu.modernmt.data.TranslationUnitMessage;
import eu.modernmt.decoder.neural.memory.lucene.DefaultDocumentBuilder;
import eu.modernmt.decoder.neural.memory.lucene.DocumentBuilder;
import eu.modernmt.decoder.neural.memory.lucene.query.DefaultQueryBuilder;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.memory.ScoreEntry;
import eu.modernmt.memory.TranslationMemory;
import java.util.Collections;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Created by davide on 03/08/17. */
public class TestLuceneTranslationMemory_hash {

  private TLuceneTranslationMemory memory;

  private Document create(
      LanguageDirection language, int memory, String sentence, String translation, String hash) {
    TranslationUnitMessage unit = addition(0, 0, memory, tu(language, sentence, translation));
    DefaultDocumentBuilder builder = (DefaultDocumentBuilder) this.memory.getDocumentBuilder();
    return builder.create(unit, hash);
  }

  @Before
  public void setup() throws Throwable {
    this.memory = new TLuceneTranslationMemory();
  }

  @After
  public void teardown() throws Throwable {
    this.memory.close();
    this.memory = null;
  }

  @Test
  public void queryWithMisleadingHashes() throws Throwable {
    IndexWriter indexWriter = memory.getIndexWriter();
    DocumentBuilder documentBuilder = memory.getDocumentBuilder();
    DefaultQueryBuilder queryBuilder = (DefaultQueryBuilder) memory.getQueryBuilder();

    indexWriter.addDocument(create(EN__IT, 2, "2-1", "2-1", "A B C D"));
    indexWriter.addDocument(create(EN__IT, 1, "1-1", "1-1", "A B C D"));
    indexWriter.addDocument(create(EN__FR, 1, "1-1F", "1-1F", "A B C D"));
    indexWriter.addDocument(create(EN__IT, 1, "1-2", "1-2", "D C B A"));
    indexWriter.addDocument(create(EN__IT, 1, "1-3", "1-3", "D C B Z"));
    indexWriter.commit();

    Query query = queryBuilder.getByMatchHash(documentBuilder, 1, "A B C D");

    IndexSearcher searcher = memory.getIndexSearcher();
    ScoreDoc[] result = searcher.search(query, 10).scoreDocs;

    assertEquals(2, result.length);

    ScoreEntry e1 = documentBuilder.asScoreEntry(searcher.doc(result[0].doc));
    ScoreEntry e2 = documentBuilder.asScoreEntry(searcher.doc(result[1].doc));

    if ("fr".equals(e1.language.target.getLanguage())) {
      assertArrayEquals(new String[] {"1-1F"}, e1.sentenceTokens);
      assertArrayEquals(new String[] {"1-1"}, e2.sentenceTokens);
    } else {
      assertArrayEquals(new String[] {"1-1F"}, e2.sentenceTokens);
      assertArrayEquals(new String[] {"1-1"}, e1.sentenceTokens);
    }
  }

  @Test
  public void overwriteNotExisting() throws Throwable {
    TranslationUnitMessage original = addition(0, 0L, 1L, tu(EN__IT, "hello world", "ciao mondo"));
    memory.onDataReceived(original);

    TranslationUnitMessage overwrite =
        overwrite(
            0,
            1L,
            1L,
            tu(EN__IT, "test sentence", "frase di prova"),
            "hello world __",
            "ciao mondo __");
    memory.onDataReceived(overwrite);

    Set<TranslationMemory.Entry> expectedEntries =
        TLuceneTranslationMemory.asEntrySet(original, overwrite);

    assertEquals(expectedEntries, memory.entrySet());
  }

  @Test
  public void overwriteExisting() throws Throwable {
    TranslationUnitMessage original = addition(0, 0L, 1L, tu(EN__IT, "hello world", "ciao mondo"));
    memory.onDataReceived(Collections.singletonList(original));

    TranslationUnitMessage overwrite =
        overwrite(
            0, 1L, 1L, tu(EN__IT, "test sentence", "frase di prova"), "hello world", "ciao mondo");
    memory.onDataReceived(Collections.singletonList(overwrite));

    Set<TranslationMemory.Entry> expectedEntries =
        TLuceneTranslationMemory.asEntrySet(Collections.singletonList(overwrite));

    assertEquals(expectedEntries, memory.entrySet());
  }
}
