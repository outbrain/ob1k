package com.outbrain.ob1k.server.filters;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.RequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * Time: 12/23/13 3:08 PM
 *
 * @author Eran Harel
 */
public class MetricsTimerFilter<T> implements AsyncFilter<T, AsyncRequestContext> {

  private final MetricFactory metricFactory;

  public MetricsTimerFilter(final MetricFactory metricFactory) {
    this.metricFactory = Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
  }

  @Override
  public ComposableFuture<T> handleAsync(final AsyncRequestContext ctx) {
    final Timer.Context time = createTimer(ctx);

    final ComposableFuture<T> futureResult = ctx.invokeAsync();
    futureResult.consume(result -> time.stop());

    return futureResult;
  }

  private Timer.Context createTimer(final RequestContext ctx) {
    return metricFactory.createTimer(ctx.getServiceClassName(), ctx.getServiceMethodName()).time();
  }
}
