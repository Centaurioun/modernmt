package eu.modernmt.processing.tokenizer;

import eu.modernmt.io.RuntimeIOException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

public class StatisticalChineseAnnotator implements BaseTokenizer.Annotator {

  private static class Dictionary {

    private static Dictionary load(String filename) throws IOException {
      InputStream stream = null;

      try {
        HashSet<String> words = new HashSet<>(21000);
        int maxLength = 0;

        stream = StatisticalChineseAnnotator.class.getResourceAsStream(filename);

        LineIterator lines = IOUtils.lineIterator(stream, StandardCharsets.UTF_8);
        while (lines.hasNext()) {
          String line = lines.nextLine();

          words.add(line);
          maxLength = Math.max(maxLength, line.length());
        }

        return new Dictionary(maxLength, words);
      } finally {
        IOUtils.closeQuietly(stream);
      }
    }

    private final int maxLength;
    private final HashSet<String> words;

    private Dictionary(int maxLength, HashSet<String> words) {
      this.maxLength = maxLength;
      this.words = words;
    }

    public boolean contains(String word) {
      return words.contains(word);
    }

    public int getWordMaxLength() {
      return maxLength;
    }
  }

  private static Dictionary dictionary = null;

  private static Dictionary getDictionary() {
    if (dictionary == null) {
      synchronized (StatisticalChineseAnnotator.class) {
        if (dictionary == null) {
          try {
            dictionary = Dictionary.load("chinese-words.list");
          } catch (IOException e) {
            throw new RuntimeIOException(e);
          }
        }
      }
    }

    return dictionary;
  }

  @Override
  public void annotate(TokenizedString string) {
    Dictionary dictionary = getDictionary();
    String text = string.toString();

    int i = 0;

    while (i < text.length() - 1) {
      int length;

      for (length = dictionary.getWordMaxLength(); length > 1; length--) {
        if (i + length > text.length()) continue;

        String word = text.substring(i, i + length);
        if (dictionary.contains(word)) {
          string.protect(i + 1, i + length);
          break;
        }
      }

      i += length;
    }
  }
}
