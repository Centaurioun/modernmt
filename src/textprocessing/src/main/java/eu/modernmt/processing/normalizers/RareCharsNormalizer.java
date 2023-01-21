package eu.modernmt.processing.normalizers;

import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.string.SentenceBuilder;
import java.util.Map;

/**
 * Created by davide on 19/02/16. Updated by andrearossi on 01/03/2017
 *
 * <p>A RareCharsNormalizer has the responsibility to know what rare characters in our input strings
 * should be replaced with other characters, to know how (with what characters) they should be
 * replaced and to actively request all the necessary replacements.
 */
public class RareCharsNormalizer extends TextProcessor<SentenceBuilder, SentenceBuilder> {

  /**
   * Method that, given a SentenceBuilder with the string to process, extracts the string, scans it
   * and requests the replacement of all rare characters that may occur in the string.
   *
   * @param builder a SentenceBuilder that holds the input String and can generate Editors to
   *     process it
   * @param metadata additional information on the current pipe (not used in this specific
   *     operation)
   * @return the SentenceBuilder received as a parameter; its internal state has been updated by the
   *     queue of the call() method
   */
  @Override
  public SentenceBuilder call(SentenceBuilder builder, Map<String, Object> metadata) {
    char[] source = builder.toCharArray();
    SentenceBuilder.Editor editor = builder.edit();

    for (int i = 0; i < source.length; i++) {
      char c = source[i];

      char nc = normalized(c);
      if (nc != '\0') editor.replace(i, 1, Character.toString(nc));
    }

    return editor.commit();
  }

  /**
   * Method that checks if a character is rare; if it is, it should be replaced and the method
   * returns the replacement character. Else, it returns a special character.
   *
   * @param c the character to check
   * @return the replacement character, if the old character must be replaced, or the special
   *     character '\0' if it mustn't,
   */
  private static char normalized(char c) {
    char nc = '\0';

    switch (c) {
      case '`':
      case '‘':
      case '’':
        nc = '\'';
        break;
      case '«':
      case '»':
      case '“':
      case '”':
      case '„':
        nc = '"';
        break;
      case '–':
      case '—':
        nc = '-';
        break;
      default:
        break;
    }

    return nc;
  }
}
