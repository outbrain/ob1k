package com.outbrain.ob1k.cache.metrics;

import java.util.concurrent.TimeoutException;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Holds a set of metrics used for measuring cache operations.
 *
 * @author hunchback
 */
public class AsyncOperationMetrics {
  private final Counter errors;
  private final Counter timeouts;
  private final Timer latency;

  public AsyncOperationMetrics(final MetricFactory metricFactory, final String component) {
    errors = metricFactory.createCounter(component, "errors");
    timeouts = metricFactory.createCounter(component, "timeouts");
    latency = metricFactory.createTimer(component, "latency");
  }

  public Timer.Context start() {
    return latency.time();
  }

  public void finished(final Try res, final Timer.Context timer) {
    timer.stop();
    if (! res.isSuccess()) {
      final Throwable ex = res.getError();
      errors.inc();
      if (ex instanceof TimeoutException) {
        timeouts.inc();
      }
    }
  }
}

