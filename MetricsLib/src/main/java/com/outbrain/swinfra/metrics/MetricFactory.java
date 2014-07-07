package com.outbrain.swinfra.metrics;

import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Timer;

/**
 * Interface for abstracting the creation of well-formed Metrics-based objects. The consumer needs only to specify the "allowable" fields (i.e., component and method) so that 
 * the metrics path is well defined (primarily concerned with graphite trees).
 *
 * @author erez
 *
 */
public interface MetricFactory {

  // TODO the commented out methods were removed because they are slightly OB related, and will make the transition to metrics 3 harder

  Timer createTimer(final String component, final String methodName);

//  Timer createTimer(String component, String methodName, MetricsVerbosityLevel metricsVerbosityLevel);

  Counter createCounter(final String component, final String methodName);

  <T> Gauge<T> createGauge(String component, String methodName, Gauge<T> metric);

  Meter createMeter(String component, String methodName, String eventType);

  Histogram createHistogram(String component, String methodName, boolean biased);

//  Histogram createHistogram(String component, String methodName, boolean biased, MetricsVerbosityLevel metricsVerbosityLevel);
//
//  void createStatefulGauge(String component, String methodName, GaugeStateHolder monitoredObject);
//
//  public <T> SettableGauge<T> createManualUpdateGauge(final String component, final String methodName, T value);
}
