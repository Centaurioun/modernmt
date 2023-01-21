package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.abbr.AbbreviationAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import java.io.Reader;

public class TamilTokenizer extends BaseTokenizer {

  public TamilTokenizer() {
    super.annotators.add(AbbreviationAnnotator.getInstance(Language.TAMIL));
    super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
  }
}
