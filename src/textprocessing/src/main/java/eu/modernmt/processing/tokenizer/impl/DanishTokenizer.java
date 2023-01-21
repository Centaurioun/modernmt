package eu.modernmt.processing.tokenizer.impl;

import eu.modernmt.lang.Language;
import eu.modernmt.processing.tokenizer.BaseTokenizer;
import eu.modernmt.processing.tokenizer.jflex.annotators.CommonTermsTokenAnnotator;
import eu.modernmt.processing.tokenizer.opennlp.OpenNLPTokenAnnotator;
import java.io.Reader;

public class DanishTokenizer extends BaseTokenizer {

  public DanishTokenizer() {
    super.annotators.add(OpenNLPTokenAnnotator.forLanguage(Language.DANISH));
    super.annotators.add(new CommonTermsTokenAnnotator((Reader) null));
  }
}
