package com.outbrain.swinfra.metrics.codahale3;

/**
 * Created by aronen on 8/18/14.
 */
public class Histogram implements com.outbrain.swinfra.metrics.api.Histogram {
  private final com.codahale.metrics.Histogram histogram;

  public Histogram(com.codahale.metrics.Histogram histogram) {
    this.histogram = histogram;
  }

  @Override
  public void update(int value) {
    histogram.update(value);
  }

  @Override
  public void update(long value) {
    histogram.update(value);
  }
}
