package eu.modernmt.processing.concurrent;

import eu.modernmt.lang.LanguageDirection;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.ProcessingPipeline;
import eu.modernmt.processing.builder.PipelineBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

/** Created by davide on 31/05/16. */
public class PipelineExecutor<P, R> {

  private final PipelineQueue<P, R> pipelines;
  private final ExecutorService executor;
  private final int threads;

  public PipelineExecutor(PipelineBuilder<P, R> builder, int threads) {
    this.pipelines = new PipelineQueue<>(builder);
    this.executor =
        threads > 1 ? Executors.newFixedThreadPool(threads) : Executors.newSingleThreadExecutor();
    this.threads = threads;
  }

  public R process(LanguageDirection language, P input) throws ProcessingException {
    return process(Collections.emptyMap(), language, input);
  }

  public R process(Map<String, Object> metadata, LanguageDirection language, P input)
      throws ProcessingException {
    ProcessingPipeline<P, R> pipeline = pipelines.get(language);

    try {
      return pipeline.call(input, metadata);
    } finally {
      pipelines.release(language, pipeline);
    }
  }

  public R[] processBatch(LanguageDirection language, P[] batch, R[] output)
      throws ProcessingException, InterruptedException {
    return processBatch(Collections.emptyMap(), language, batch, output);
  }

  public R[] processBatch(
      Map<String, Object> metadata, LanguageDirection language, P[] batch, R[] output)
      throws ProcessingException, InterruptedException {
    Future<?>[] locks = new Future<?>[threads];

    if (batch.length < threads) {
      locks[0] =
          executor.submit(new FragmentTask(metadata, language, batch, output, 0, batch.length));
    } else {
      int fragmentSize = batch.length / threads;

      for (int i = 0; i < threads; i++) {
        int offset = i * fragmentSize;
        int length = fragmentSize;

        if (i == threads - 1) length = batch.length - offset;

        locks[i] =
            executor.submit(new FragmentTask(metadata, language, batch, output, offset, length));
      }
    }

    for (Future<?> lock : locks) {
      if (lock == null) break;

      try {
        lock.get();
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();

        if (cause instanceof ProcessingException) throw (ProcessingException) cause;
        else if (cause instanceof RuntimeException) throw (RuntimeException) cause;
        else throw new Error("Unexpected exception", cause);
      }
    }

    return output;
  }

  public void shutdown() {
    executor.shutdown();
  }

  public boolean awaitTermination(int timeout, TimeUnit unit) throws InterruptedException {
    return executor.awaitTermination(timeout, unit);
  }

  public void shutdownNow() {
    executor.shutdownNow();
  }

  public class FragmentTask implements Callable<Void> {

    private final Map<String, Object> metadata;
    private final LanguageDirection language;
    private final P[] batch;
    private final Object[] output;
    private final int offset;
    private final int length;

    public FragmentTask(
        Map<String, Object> metadata,
        LanguageDirection language,
        P[] batch,
        R[] output,
        int offset,
        int length) {
      this.metadata = metadata;
      this.language = language;
      this.batch = batch;
      this.output = output;
      this.offset = offset;
      this.length = length;
    }

    @Override
    public Void call() throws ProcessingException {
      ProcessingPipeline<P, R> pipeline = pipelines.get(language);

      try {
        for (int i = 0; i < length; i++) {
          output[offset + i] = pipeline.call(batch[offset + i], metadata);
          batch[offset + i] = null; // free memory
        }

        return null;
      } finally {
        pipelines.release(language, pipeline);
      }
    }
  }
}
