package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Holds a set of metrics used for measuring cache operations.
 *
 * @author hunchback
 */
public class MetricOpCache extends MetricOpBase {
  private final Counter hit;
  private final Counter miss;

  public MetricOpCache(final MetricFactory metricFactory, final String component,
                       final String prefix) {
    super(metricFactory, component, prefix);
    hit = metricFactory.createCounter(component, prefix + "HitCount");
    miss = metricFactory.createCounter(component, prefix + "MissCount");
  }

  @Override
  public void finished(Try<?> res, final Timer.Context timer) {
    finished(res, timer, hit, miss, errors, timeouts);
  }
}

