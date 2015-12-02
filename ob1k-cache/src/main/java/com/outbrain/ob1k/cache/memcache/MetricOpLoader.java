package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Holds a set of metrics used for measuring load operations.
 *
 * @author hunchback
 */
public class MetricOpLoader extends MetricOpBase {
  private final Counter success;

  public MetricOpLoader(final MetricFactory metricFactory, final String component,
                        final String prefix) {
    super(metricFactory, component, prefix);
    success = metricFactory.createCounter(component, prefix + "SuccessCount");
  }

  @Override
  public void finished(final Try<?> res, final Timer.Context timer) {
    finished(res, timer, success, success, errors, timeouts);
  }
}

