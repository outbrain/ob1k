package com.outbrain.swinfra.metrics.codahale3;

/**
 * Created by aronen on 8/18/14.
 */
public class Meter implements com.outbrain.swinfra.metrics.api.Meter {
  private final com.codahale.metrics.Meter meter;

  public Meter(com.codahale.metrics.Meter meter) {
    this.meter = meter;
  }

  @Override
  public void mark() {
    meter.mark();
  }

  @Override
  public void mark(long n) {
    meter.mark(n);
  }
}
