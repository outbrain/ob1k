package com.outbrain.ob1k.cache.metrics;

import java.util.concurrent.TimeoutException;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromTry;

/**
 * Holds a set of metrics used for measuring cache operations.
 *
 * @author hunchback
 */
public class AsyncOperationMetrics<T> {
  private final Counter errors;
  private final Counter timeouts;
  private final Timer latency;

  public AsyncOperationMetrics(final MetricFactory metricFactory, final String component) {
    errors = metricFactory.createCounter(component, "errors");
    timeouts = metricFactory.createCounter(component, "timeouts");
    latency = metricFactory.createTimer(component, "latency");
  }

  public ComposableFuture<T> update(ComposableFuture<T> future) {
    final Timer.Context tc = latency.time();
    return future.continueWith(new FutureResultHandler<T, T>() {
      @Override
      public ComposableFuture<T> handle(final Try<T> result) {
        tc.stop();
        if (!result.isSuccess()) {
          errors.inc();
          if (result.getError() instanceof TimeoutException) {
            timeouts.inc();
          }
        }
        return fromTry(result);
      }
    });
  }
}
