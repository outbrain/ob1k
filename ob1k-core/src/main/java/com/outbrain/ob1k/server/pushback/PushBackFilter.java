package com.outbrain.ob1k.server.pushback;

import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.SyncRequestContext;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.ExecutionException;

/**
 * Filter that will push back requests based on a strategy
 *
 * @see PushBackStrategy
 *
 * @param <T> type of response of Filter
 */
public class PushBackFilter<T> implements AsyncFilter<T, AsyncRequestContext>, SyncFilter<T, SyncRequestContext> {

  private final PushBackStrategy pushBackStrategy;
  private final Counter pushBackCounter;

  public PushBackFilter(final PushBackStrategy pushBackStrategy, final String component, final MetricFactory metricFactory) {
    this.pushBackStrategy = pushBackStrategy;
    this.pushBackCounter = metricFactory.createCounter(component, "pushBackCounter");
  }

  @Override
  public ComposableFuture<T> handleAsync(final AsyncRequestContext ctx) {
    final ComposableFuture<T> result;
    final boolean pushBack = pushBackStrategy.allowRequest();
    try {
      if (pushBack) {
        result = ctx.invokeAsync();
      } else {
        pushBackCounter.inc();
        result = ComposableFutures.fromError(pushBackStrategy.generateExceptionOnPushBack());
      }
    } finally {
      pushBackStrategy.done(pushBack);
    }
    return result;
  }

  @Override
  public T handleSync(final SyncRequestContext ctx) throws ExecutionException {
    final boolean pushBack = pushBackStrategy.allowRequest();
    try {
      if (pushBack) {
        return ctx.invokeSync();
      } else {
        pushBackCounter.inc();
        throw pushBackStrategy.generateExceptionOnPushBack();
      }
    } finally {
      pushBackStrategy.done(pushBack);
    }
  }
}
