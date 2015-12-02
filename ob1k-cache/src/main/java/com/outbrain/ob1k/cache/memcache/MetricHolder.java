package com.outbrain.ob1k.cache.memcache;

import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

/**
 * Holds all metrics used for monitoring MemcacheClient.
 *
 * @author hunchback
 */
public class MetricHolder {
  protected MetricOpCache cache;
  protected MetricOpLoader load;
  protected Counter delete;

  public MetricOpCache getCache() {
    return cache;
  }

  public MetricOpLoader getLoad() {
    return load;
  }

  public Counter getDelete() {
    return delete;
  }

  public MetricHolder(final String component, MetricFactory metricFactory) {
      if (metricFactory == null) {
        metricFactory = mock(MetricFactory.class);
        when(metricFactory.createCounter(any(String.class),any(String.class))).thenReturn(mock(Counter.class));
        when(metricFactory.createTimer(any(String.class),any(String.class))).thenReturn(mock(Timer.class));
      }
      cache = new MetricOpCache(metricFactory, component, "cache");
      load = new MetricOpLoader(metricFactory, component, "load");
      delete = metricFactory.createCounter(component, "cancelOperationCount");
  }
}
