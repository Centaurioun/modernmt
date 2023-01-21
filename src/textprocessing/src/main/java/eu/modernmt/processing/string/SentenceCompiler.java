package eu.modernmt.processing.string;

import eu.modernmt.model.Sentence;
import eu.modernmt.model.Tag;
import eu.modernmt.processing.TextProcessor;
import eu.modernmt.processing.tags.format.InputFormat;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by andrea on 02/03/17.
 *
 * <p>A SentenceCompiler is a TextProcessor that can create SentenceObjects by requesting their
 * building to a SentenceBuilder instance.
 */
public class SentenceCompiler extends TextProcessor<SentenceBuilder, Sentence> {

  public static final String INPUT_FORMAT_TYPE = "SentenceCompiler.INPUT_FORMAT_TYPE";

  private final Logger logger = LogManager.getLogger(SentenceCompiler.class);

  /**
   * This method asks the SentenceBuilder to generate a Sentence object, starting from the
   * Transformations list filled by the others TextProcessors during the previous processing phases
   *
   * @param builder the SentenceBuilder with the current string and the list of Transformations to
   *     execute, and is ready to generate Tokens and build the Sentence
   * @param metadata additional information on the current pipe (not used in this specific
   *     operation)
   * @return the Sentence generated by the SentenceBuilder
   */
  @Override
  public Sentence call(SentenceBuilder builder, Map<String, Object> metadata) {
    Sentence sentence = builder.build();
    builder.clear();

    if (sentence.hasTags()) {
      InputFormat.Type type = (InputFormat.Type) metadata.get(INPUT_FORMAT_TYPE);
      Tag[] tags = sentence.getTags();

      InputFormat format = getInputFormat(tags, type);

      if (logger.isDebugEnabled())
        logger.debug("Transforming <xml> with InputFormat: " + format.getClass().getSimpleName());

      format.transform(tags);
    }

    return sentence;
  }

  private static InputFormat getInputFormat(Tag[] tags, InputFormat.Type type) {
    return type == null ? InputFormat.auto(tags) : InputFormat.build(type);
  }
}
