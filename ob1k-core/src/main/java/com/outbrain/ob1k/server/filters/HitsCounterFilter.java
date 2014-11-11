package com.outbrain.ob1k.server.filters;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.RequestContext;
import com.outbrain.ob1k.SyncRequestContext;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.OnResultHandler;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 10/2/14.
 *
 * counts success and failure per endpoint.
 */
public class HitsCounterFilter<T> implements AsyncFilter<T, AsyncRequestContext>, SyncFilter<T, SyncRequestContext> {
  private final MetricFactory metricFactory;

  public HitsCounterFilter (final MetricFactory metricFactory) {
    this.metricFactory = Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
  }

  @Override
  public ComposableFuture<T> handleAsync(final AsyncRequestContext ctx) {
    final ComposableFuture<T> futureResult = ctx.invokeAsync();
    futureResult.onResult(new OnResultHandler<T>() {
      @Override
      public void handle(ComposableFuture<T> result) {
        getTotalCounter(ctx).inc();
        if (result.isSuccess()) {
          getSuccessCounter(ctx).inc();
        } else {
          getErrorCounter(ctx).inc();
        }
      }
    });

    return futureResult;
  }

  @Override
  public T handleSync(SyncRequestContext ctx) throws ExecutionException {
    try {
      final T res = ctx.invokeSync();
      getSuccessCounter(ctx).inc();
      return res;
    } catch (ExecutionException | RuntimeException e) {
      getErrorCounter(ctx).inc();
      throw e;
    } finally {
      getTotalCounter(ctx).inc();
    }
  }

  private Counter getSuccessCounter(final RequestContext ctx) {
    return metricFactory.createCounter(ctx.getServiceClassName(), ctx.getServiceMethodName() + ".success");
  }

  private Counter getErrorCounter(final RequestContext ctx) {
    return metricFactory.createCounter(ctx.getServiceClassName(), ctx.getServiceMethodName() + ".error");
  }

  private Counter getTotalCounter(final RequestContext ctx) {
    return metricFactory.createCounter(ctx.getServiceClassName(), ctx.getServiceMethodName() + ".total");
  }

}
