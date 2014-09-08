package com.outbrain.ob1k.server.build;


import com.outbrain.swinfra.metrics.api.MetricFactory;

/**
 * Created by aronen on 7/20/14.
 */
public interface ExtraParamsPhase {
  ExtraParamsPhase acceptKeepAlive(final boolean keepAlive);
  ExtraParamsPhase supportZip(final boolean useZip);
  ExtraParamsPhase setMaxContentLength(final int maxContentLength);
  ExtraParamsPhase configureExecutorService(final int minSize, final int maxSize);
  ExtraParamsPhase setMetricFactory(final MetricFactory metricFactory);
}
