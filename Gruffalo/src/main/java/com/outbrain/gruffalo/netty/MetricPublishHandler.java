package com.outbrain.gruffalo.netty;

import org.springframework.util.Assert;

import com.outbrain.gruffalo.publish.MetricsPublisher;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
class MetricPublishHandler extends SimpleChannelInboundHandler<Batch> {

  private final MetricsPublisher publisher;
  private final Timer publishTimer;
  private final Counter metricsCounter;

  public MetricPublishHandler(final MetricsPublisher publisher, final MetricFactory metricFactory) {
    Assert.notNull(publisher, "publisher may not be null");
    Assert.notNull(metricFactory, "metricFactory may not be null");
    this.publisher = publisher;
    String component = getClass().getSimpleName();
    publishTimer = metricFactory.createTimer(component, "publishMetricsBatch");
    metricsCounter = metricFactory.createCounter(component, "metricsSent");
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, final Batch msg) throws Exception {
    Timer.Context timerContext = publishTimer.time();
    try {
      publisher.publishMetrics(msg.payload.toString());
      metricsCounter.inc(msg.batchSize);
    } finally {
      timerContext.stop();
    }
  }

}
