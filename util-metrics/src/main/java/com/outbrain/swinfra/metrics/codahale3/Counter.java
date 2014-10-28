package com.outbrain.swinfra.metrics.codahale3;

/**
 * Created by aronen on 8/18/14.
 */
public class Counter implements com.outbrain.swinfra.metrics.api.Counter {
  private final com.codahale.metrics.Counter counter;

  public Counter(com.codahale.metrics.Counter counter) {
    this.counter = counter;
  }

  @Override
  public void inc() {
    counter.inc();

  }

  @Override
  public void inc(long n) {
    counter.inc(n);
  }

  @Override
  public void dec() {
    counter.dec();
  }

  @Override
  public void dec(long n) {
    counter.dec(n);
  }

  @Override
  public long getCount() {
    return counter.getCount();
  }
}
