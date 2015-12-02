package com.outbrain.ob1k.cache.memcache;

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
public abstract class MetricOpBase {
  protected final Counter errors;
  protected final Counter timeouts;
  protected final Timer elapsed;

  public MetricOpBase(final MetricFactory metricFactory, final String component,
                      final String prefix) {
    errors = metricFactory.createCounter(component, prefix + "ErrorsCount");
    timeouts = metricFactory.createCounter(component, prefix + "TimeoutsCount");
    elapsed = metricFactory.createTimer(component, prefix + "ElapsedTime");
  }

  public Timer.Context start() {
    return elapsed.time();
  }

  public void finished(final Try<?> res, final Timer.Context timer,
                       Counter hit, Counter miss, Counter err, Counter timeout) {
    timer.stop();
    if (res.isSuccess()) {
      if (res.getValue() == null) {
        err.inc();
      } else {
        hit.inc();
      }
    } else {
      final Throwable error = res.getError();
      err.inc();
      if (error instanceof TimeoutException) {
        timeout.inc();
      }
    }
  }

  public abstract void finished(Try<?> res, final Timer.Context timer);
}

