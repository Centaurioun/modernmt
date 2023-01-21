package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.StatisticalChineseAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import java.io.Reader;

public class ChineseTokenizer extends BaseTokenizer {

  public ChineseTokenizer() {
    super.annotators.add(new StatisticalChineseAnnotator());
    super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
  }
}
