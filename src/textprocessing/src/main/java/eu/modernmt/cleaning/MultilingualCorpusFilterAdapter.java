package eu.modernmt.cleaning;

import eu.modernmt.lang.Language;
import eu.modernmt.model.corpus.TranslationUnit;
import java.util.HashMap;

public class MultilingualCorpusFilterAdapter implements MultilingualCorpusFilter {

  public interface Factory {

    CorpusFilter create();
  }

  private final Factory factory;
  private final boolean hasInitializer;
  private final HashMap<Language, CorpusFilter> filters = new HashMap<>();

  public MultilingualCorpusFilterAdapter(Class<? extends CorpusFilter> clazz) {
    this(
        () -> {
          try {
            return clazz.newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException(e);
          }
        });
  }

  public MultilingualCorpusFilterAdapter(Factory factory) {
    this.factory = factory;
    this.hasInitializer = (factory.create().getInitializer() != null);
  }

  @Override
  public Initializer getInitializer() {
    if (!hasInitializer) return null;

    HashMap<Language, CorpusFilter.Initializer> initializers = new HashMap<>();

    return new Initializer() {
      @Override
      public void onBegin() {
        // Nothing to do
      }

      @Override
      public void onTranslationUnit(TranslationUnit tu, int index) {
        initializers
            .computeIfAbsent(tu.language.source, this::createInitializer)
            .onLine(tu.language.source, tu.source, index);
        initializers
            .computeIfAbsent(tu.language.target, this::createInitializer)
            .onLine(tu.language.target, tu.target, index);
      }

      private CorpusFilter.Initializer createInitializer(Language language) {
        CorpusFilter filter = filters.computeIfAbsent(language, (l) -> factory.create());
        CorpusFilter.Initializer initializer = filter.getInitializer();
        initializer.onBegin();

        return initializer;
      }

      @Override
      public void onEnd() {
        for (CorpusFilter.Initializer initializer : initializers.values()) initializer.onEnd();
      }
    };
  }

  @Override
  public boolean accept(TranslationUnit tu, int index) {
    CorpusFilter sourceFilter =
        filters.computeIfAbsent(tu.language.source, (l) -> factory.create());
    CorpusFilter targetFilter =
        filters.computeIfAbsent(tu.language.target, (l) -> factory.create());

    return sourceFilter.accept(tu.language.source, tu.source, index)
        && targetFilter.accept(tu.language.target, tu.target, index);
  }

  @Override
  public void clear() {
    for (CorpusFilter filter : filters.values()) filter.clear();
  }
}
