package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.corenlp.CoreNLPTokenAnnotator;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import java.io.Reader;

public class ArabicTokenizer extends BaseTokenizer {

  public ArabicTokenizer() {

    super.annotators.add(CoreNLPTokenAnnotator.forLanguage(Language.ARABIC));
    super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
  }
}
