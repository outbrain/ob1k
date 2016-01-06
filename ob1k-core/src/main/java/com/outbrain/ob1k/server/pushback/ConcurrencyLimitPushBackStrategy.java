package com.outbrain.ob1k.server.pushback;

import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrencyLimitPushBackStrategy implements PushBackStrategy {

  private final int concurrencyLimit;
  private final AtomicInteger concurrencyCounter = new AtomicInteger(0);
  private final AtomicInteger concurrencyHighWaterMark = new AtomicInteger(0);
  
  public ConcurrencyLimitPushBackStrategy(final int concurrencyLimit, final String component, final MetricFactory metricFactory) {
    this.concurrencyLimit = concurrencyLimit;
    metricFactory.registerGauge(component, "concurrencyCounter", concurrencyCounter::get);
    metricFactory.registerGauge(component, "concurrencyHighWaterMark", this::readHighWaterMark);
  }

  @Override
  public boolean allowRequest() {
    final int currentConcurrency = concurrencyCounter.getAndIncrement();
    concurrencyHighWaterMark.set(Math.max(currentConcurrency, concurrencyHighWaterMark.get()));
    return currentConcurrency < concurrencyLimit;
  }

  @Override
  public void done(final boolean allowedRequest) {
    concurrencyCounter.getAndDecrement();
  }

  @Override
  public PushBackException generateExceptionOnPushBack() {
    return new PushBackException("Reached concurrency limit " + concurrencyLimit);
  }

  private Integer readHighWaterMark() {
    return concurrencyHighWaterMark.getAndSet(0);
  }
}
