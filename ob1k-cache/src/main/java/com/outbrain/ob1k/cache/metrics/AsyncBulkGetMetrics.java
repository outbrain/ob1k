package com.outbrain.ob1k.cache.metrics;

import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import java.util.Map;

/**
 * Holds a set of metrics used for measuring get operations.
 *
 * @author hunchback
 */
public class AsyncBulkGetMetrics extends AsyncOperationMetrics {
  private final Counter hits;
  private final Counter total;

  public AsyncBulkGetMetrics(final MetricFactory metricFactory, final String component) {
    super(metricFactory, component);
    hits = metricFactory.createCounter(component, "cacheHits");
    total = metricFactory.createCounter(component, "cacheTotal");
  }

  public void increaseTotal(final Iterable keys) {
    for (final Object key : keys) {
      total.inc();
    }
  }

  @Override
  public void finished(final Try res, final Timer.Context timer) {
    super.finished(res, timer);

    if (res.isSuccess()) {
      if (res.getValue() != null) {
        final Map map = (Map) res.getValue();
        hits.inc(map.size());
      }
    }
  }
}

