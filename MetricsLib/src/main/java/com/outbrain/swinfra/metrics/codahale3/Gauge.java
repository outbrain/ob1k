package com.outbrain.swinfra.metrics.codahale3;

/**
 * Created by aronen on 8/18/14.
 */
public class Gauge<T> implements com.outbrain.swinfra.metrics.api.Gauge<T> {
  private final com.codahale.metrics.Gauge<T> gauge;

  public Gauge(com.codahale.metrics.Gauge<T> gauge) {
    this.gauge = gauge;
  }

  @Override
  public T getValue() {
    return gauge.getValue();
  }
}
