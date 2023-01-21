package eu.modernmt.decoder.neural.memory.lucene.utils;

import eu.modernmt.decoder.neural.memory.lucene.LuceneTranslationMemory;
import java.io.File;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;

/** Created by davide on 12/02/18. */
public class Dump {

  public static void main(String[] args) throws Throwable {
    if (args.length != 1)
      throw new IllegalArgumentException("Wrong number of arguments, usage: <model-path>");

    LuceneTranslationMemory memory = new LuceneTranslationMemory(new File(args[0]), 1);
    memory.dumpAll(
        entry -> {
          String str =
              StringUtils.join(
                  new String[] {
                    Objects.toString(entry.tuid),
                    Long.toString(entry.memory),
                    entry.language.source.toLanguageTag(),
                    entry.language.target.toLanguageTag(),
                    entry.sentence.replace('\t', ' '),
                    entry.translation.replace('\t', ' ')
                  },
                  '\t');

          System.out.println(str);
        });
  }
}
