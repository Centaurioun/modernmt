package eu.modernmt.api.actions.translation;

import eu.modernmt.api.actions.util.ContextUtils;
import eu.modernmt.api.framework.*;
import eu.modernmt.api.framework.actions.ObjectAction;
import eu.modernmt.api.framework.routing.Route;
import eu.modernmt.api.model.ContextVectorResult;
import eu.modernmt.context.ContextAnalyzerException;
import eu.modernmt.facade.ModernMT;
import eu.modernmt.io.FileProxy;
import eu.modernmt.lang.Language;
import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.model.ContextVector;
import eu.modernmt.persistence.PersistenceException;
import java.io.*;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/** Created by davide on 15/12/15. */
@Route(aliases = "context-vector", method = HttpMethod.GET)
public class GetContextVector extends ObjectAction<ContextVectorResult> {

  private static File copy(FileProxy source) throws IOException {
    File destination = File.createTempFile("mmt-context", "txt");

    InputStream input = null;
    OutputStream output = null;

    try {
      input = source.getInputStream();
      output = new FileOutputStream(destination, false);

      IOUtils.copyLarge(input, output);
    } finally {
      IOUtils.closeQuietly(input);
      IOUtils.closeQuietly(output);
    }

    return destination;
  }

  @Override
  protected ContextVectorResult execute(RESTRequest req, Parameters _params)
      throws ContextAnalyzerException, PersistenceException, IOException {
    Params params = (Params) _params;
    Map<Language, ContextVector> contexts;

    File temp = null;

    try {

      if (params.text != null) {
        contexts =
            ModernMT.translation.getContextVectors(
                params.user, params.text, params.limit, params.source, params.targets);
      } else {
        boolean gzipped = params.compression != null;
        File file;

        if (params.localFile != null) {
          if (gzipped) temp = file = copy(FileProxy.wrap(params.localFile, true));
          else file = params.localFile;
        } else {
          temp = file = copy(new ParameterFileProxy(params.content, gzipped));
        }

        contexts =
            ModernMT.translation.getContextVectors(
                params.user, file, params.limit, params.source, params.targets);
      }
    } finally {
      if (temp != null) FileUtils.deleteQuietly(temp);
    }

    ContextUtils.resolve(contexts.values());
    return new ContextVectorResult(params.source, contexts, params.backwardCompatible);
  }

  @Override
  protected Parameters getParameters(RESTRequest req) throws Parameters.ParameterParsingException {
    return new Params(req);
  }

  public enum FileCompression {
    GZIP
  }

  public static class Params extends Parameters {

    public static final int DEFAULT_LIMIT = 10;

    public final UUID user;
    public final Language source;
    public final Language[] targets;
    public final int limit;
    public final String text;
    public final File localFile;
    public final FileParameter content;
    public final FileCompression compression;
    public final boolean backwardCompatible;

    public Params(RESTRequest req) throws ParameterParsingException {
      super(req);

      this.user = getUUID("user", null);
      this.limit = getInt("limit", DEFAULT_LIMIT);

      Language sourceLanguage = getLanguage("source", null);
      Language[] targetLanguages = getLanguageArray("targets", null);

      if (sourceLanguage == null && targetLanguages == null) {
        LanguageDirection engineDirection =
            ModernMT.getNode().getEngine().getLanguageIndex().asSingleLanguagePair();

        if (engineDirection != null) {
          this.source = engineDirection.source;
          this.targets = new Language[] {engineDirection.target};
          this.backwardCompatible = true;
        } else {
          throw new ParameterParsingException("source");
        }
      } else if (sourceLanguage == null) {
        throw new ParameterParsingException("source");
      } else if (targetLanguages == null) {
        throw new ParameterParsingException("targets");
      } else {
        this.source = sourceLanguage;
        this.targets = targetLanguages;
        this.backwardCompatible = false;
      }

      FileParameter content;
      String localFile;

      if ((content = req.getFile("content")) != null) {
        this.text = null;
        this.localFile = null;
        this.content = content;
        this.compression = getEnum("compression", FileCompression.class, null);
      } else if ((localFile = getString("local_file", false, null)) != null) {
        this.text = null;
        this.localFile = new File(localFile);
        this.content = null;
        this.compression = getEnum("compression", FileCompression.class, null);
      } else {
        this.text = getString("text", false);
        this.localFile = null;
        this.content = null;
        this.compression = null;
      }
    }
  }
}
