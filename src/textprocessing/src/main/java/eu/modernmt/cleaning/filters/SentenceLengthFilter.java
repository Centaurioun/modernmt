package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.MultilingualCorpusFilter;
import eu.modernmt.cleaning.filters.util.Sequence;
import eu.modernmt.io.WordCounter;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.corpus.TranslationUnit;
import java.util.HashMap;

/** Created by davide on 25/08/17. */
public class SentenceLengthFilter implements MultilingualCorpusFilter {

  public static final int MIN_CORPUS_LINES = 1000;
  public static final int DEFAULT_MAX_LINE_LENGTH = 1024;

  private final int maxLength;
  private HashMap<LanguageDirection, Sequence> lengthRatios = new HashMap<>();

  public SentenceLengthFilter() {
    this(DEFAULT_MAX_LINE_LENGTH);
  }

  public SentenceLengthFilter(int maxLength) {
    this.maxLength = maxLength;
  }

  @Override
  public Initializer getInitializer() {
    return new Initializer() {

      @Override
      public void onBegin() {
        clear();
      }

      @Override
      public void onTranslationUnit(TranslationUnit tu, int index) {
        int sourceLength = tu.source.length();
        int targetLength = tu.target.length();

        if (sourceLength <= maxLength
            && targetLength <= maxLength
            && sourceLength > 0
            && targetLength > 0) {
          Sequence ratioSequence = lengthRatios.computeIfAbsent(tu.language, k -> new Sequence());

          ratioSequence.add(((double) WordCounter.count(tu.source)) / WordCounter.count(tu.target));
        }
      }

      @Override
      public void onEnd() {
        // Nothing to do
      }
    };
  }

  @Override
  public boolean accept(TranslationUnit tu, int index) {
    int sourceLength = tu.source.length();
    int targetLength = tu.target.length();

    if (sourceLength > maxLength
        || targetLength > maxLength
        || sourceLength == 0
        || targetLength == 0) return false;

    Sequence seq = lengthRatios.get(tu.language);

    if (seq.length() < MIN_CORPUS_LINES) return true;

    int sourceWordCount = WordCounter.count(tu.source);
    int targetWordCount = WordCounter.count(tu.target);

    if (sourceWordCount < 6 && targetWordCount < 6) return true;

    double ratio = ((double) sourceWordCount) / targetWordCount;

    if (ratio > (seq.getAverage() + 6 * seq.getStandardDeviation())) return false;
    if (ratio < (seq.getAverage() - 6 * seq.getStandardDeviation())) return false;

    return true;
  }

  @Override
  public void clear() {
    this.lengthRatios.clear();
  }
}
