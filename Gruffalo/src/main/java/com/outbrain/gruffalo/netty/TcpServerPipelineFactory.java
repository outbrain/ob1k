package com.outbrain.gruffalo.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import org.springframework.util.Assert;

class TcpServerPipelineFactory extends ChannelInitializer<Channel> {

  private final int readerIdleTimeSeconds;
  private final LineBasedFrameDecoderFactory framerFactory;
  private final StringDecoder decoder;
  private final MetricBatcherFactory metricBatcherFactory;
  private final MetricPublishHandler publishHandler;
  private final EventExecutorGroup publishExecutor;

  public TcpServerPipelineFactory(final int readerIdleTimeSeconds, final LineBasedFrameDecoderFactory framerFactory, final StringDecoder decoder,
      final MetricBatcherFactory metricBatcherFactory, final MetricPublishHandler publishHandler, final EventExecutorGroup publishExecutor) {
    Assert.notNull(framerFactory, "framerFactory,  may not be null");
    Assert.notNull(decoder, "decoder may not be null");
    Assert.notNull(metricBatcherFactory, "metricBatcherFactory may not be null");
    Assert.notNull(publishHandler, "publishHandler may not be null");
    Assert.notNull(publishExecutor, "publishExecutor may not be null");

    this.readerIdleTimeSeconds = readerIdleTimeSeconds;
    this.framerFactory = framerFactory;
    this.decoder = decoder;
    this.metricBatcherFactory = metricBatcherFactory;
    this.publishHandler = publishHandler;
    this.publishExecutor = publishExecutor;
  }

  @Override
  protected void initChannel(final Channel channel) throws Exception {
    final ChannelPipeline pipeline = channel.pipeline();
    pipeline.addLast("idleStateHandler", new IdleStateHandler(readerIdleTimeSeconds, 0, 0));
    pipeline.addLast("framer", framerFactory.getLineFramer());
    pipeline.addLast("decoder", decoder);
    pipeline.addLast("batchHandler", metricBatcherFactory.getMetricBatcher());
    // TODO if we restore AMQP we need to restore the async execution
    pipeline.addLast(/*publishExecutor, */"publishHandler", publishHandler);
  }

}
