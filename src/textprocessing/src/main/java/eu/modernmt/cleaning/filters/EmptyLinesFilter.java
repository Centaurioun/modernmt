package eu.modernmt.cleaning.filters;

import eu.modernmt.cleaning.CorpusFilter;
import eu.modernmt.lang.Language;
import java.util.regex.Pattern;

/** Created by davide on 14/03/16. */
public class EmptyLinesFilter implements CorpusFilter {

  private static final Pattern WHITESPACE_REMOVER = Pattern.compile("\\s+");

  private static boolean isBlankLine(String line) {
    return line.isEmpty() || WHITESPACE_REMOVER.matcher(line).replaceAll("").isEmpty();
  }

  @Override
  public Initializer getInitializer() {
    return null;
  }

  @Override
  public boolean accept(Language language, String line, int index) {
    return !isBlankLine(line);
  }

  @Override
  public void clear() {
    // Nothing to do
  }
}
