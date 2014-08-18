package com.outbrain.swinfra.metrics.codahale3;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.swinfra.metrics.api.*;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.Histogram;
import com.outbrain.swinfra.metrics.api.Meter;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Created by aronen on 8/18/14.
 */
public class CodahaleMetricsFactory implements MetricFactory {
  private final MetricRegistry registry;

  public CodahaleMetricsFactory(MetricRegistry registry) {
    this.registry = registry;
  }

  @Override
  public Timer createTimer(String component, String methodName) {
    final com.codahale.metrics.Timer timer = registry.timer(MetricRegistry.name(component, methodName));
    return new com.outbrain.swinfra.metrics.codahale3.Timer(timer);
  }

  @Override
  public Counter createCounter(String component, String methodName) {
    final com.codahale.metrics.Counter counter = registry.counter(MetricRegistry.name(component, methodName));
    return new com.outbrain.swinfra.metrics.codahale3.Counter(counter);
  }

  @Override
  public <T> Gauge<T> registerGauge(String component, String methodName, final Gauge<T> gauge) {
    registry.register(MetricRegistry.name(component, methodName), new com.codahale.metrics.Gauge<T>() {
      @Override
      public T getValue() {
        return gauge.getValue();
      }
    });

    return gauge;
  }

  @Override
  public Meter createMeter(String component, String methodName, String eventType) {
    final com.codahale.metrics.Meter meter = registry.meter(MetricRegistry.name(component, methodName));
    return new com.outbrain.swinfra.metrics.codahale3.Meter(meter);
  }

  @Override
  public Histogram createHistogram(String component, String methodName, boolean biased) {
    final com.codahale.metrics.Histogram histogram = registry.histogram(MetricRegistry.name(component, methodName));
    return new com.outbrain.swinfra.metrics.codahale3.Histogram(histogram);
  }
}
