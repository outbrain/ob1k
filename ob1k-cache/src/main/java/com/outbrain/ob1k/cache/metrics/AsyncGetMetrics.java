package com.outbrain.ob1k.cache.metrics;

import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Holds a set of metrics used for measuring get operations.
 *
 * @author hunchback
 */
public class AsyncGetMetrics extends AsyncOperationMetrics {
  private final Counter hits;
  private final Counter total;

  public AsyncGetMetrics(final MetricFactory metricFactory, final String component) {
    super(metricFactory, component);
    hits = metricFactory.createCounter(component, "cacheHits");
    total = metricFactory.createCounter(component, "cacheTotal");
  }

  @Override
  public void finished(Try res, Timer.Context timer) {
    super.finished(res, timer);

    if (res.isSuccess()) {
      total.inc();
      if (res.getValue() != null) {
        hits.inc();
      }
    }
  }
}

