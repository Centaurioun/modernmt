package eu.modernmt.decoder.neural.memory.lucene.analysis;

import java.io.Reader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;

public class HashAnalyzer extends Analyzer {

  public HashAnalyzer() {
    super(GLOBAL_REUSE_STRATEGY);
  }

  @Override
  protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
    return new TokenStreamComponents(new WhitespaceTokenizer(reader));
  }
}
