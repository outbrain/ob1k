package com.outbrain.gruffalo.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.outbrain.gruffalo.util.HostName2MetricName;
import com.outbrain.swinfra.metrics.MetricFactory;
import com.yammer.metrics.core.Counter;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

/**
 * Time: 8/4/13 12:30 PM
 *
 * @author Eran Harel
 */
public class NettyGraphiteClient implements GraphiteClient {

  private static final Logger log = LoggerFactory.getLogger(NettyGraphiteClient.class);
  private final Counter errorCounter;
  private final Counter pushBackCounter;
  private final Counter reconnectCounter;
  private final String host;
  private final ChannelFutureListener errorListener = new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      if (!future.isSuccess()) {
        errorCounter.inc();
        // Under high load this is spam. Maybe add later...
        // log.error("Failed to write to {}: {}", host, future.cause().toString());
      }
    }
  };
  private GraphiteClientChannelInitializer channelInitializer;
  private volatile ChannelFuture channelFuture;

  public NettyGraphiteClient(final MetricFactory metricFactory, String host) {
    this.host = host;
    String graphiteCompatibleHostName = HostName2MetricName.graphiteCompatibleHostPortName(host);
    errorCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".errors");
    pushBackCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".pushBack");
    reconnectCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".reconnect");
    log.info("Client for [{}] initialized", host);
  }

  public void setChannelInitializer(GraphiteClientChannelInitializer channelInitializer) {
    this.channelInitializer = channelInitializer;
  }

  @Override
  public void connect() {
    reconnectCounter.inc();
    log.info("Client for [{}] is reconnecting", host);
    channelFuture = channelInitializer.connect();
  }

  @Override
  public void publishMetrics(final String metrics) {
    if (channelFuture.isDone()) {
      channelFuture.channel().writeAndFlush(metrics).addListener(errorListener);
    } else {
      channelFuture.addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(final ChannelFuture future) throws Exception {
          future.channel().writeAndFlush(metrics).addListener(errorListener);
        }
      });
    }
  }

  @Override
  public void onPushBack() {
    pushBackCounter.inc();
  }

}
