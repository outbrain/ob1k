package com.outbrain.gruffalo.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.outbrain.gruffalo.util.HostName2MetricName;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Counter;

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
  private final Counter rejectedCounter;
  private final Counter publishedCounter;
  private final String host;
  private final ChannelFutureListener opListener = new ChannelFutureListener() {
    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
      if (future.isSuccess()) {
        publishedCounter.inc();
      } else {
        errorCounter.inc();
        // Under high load this is spam. Maybe add later...
        // log.error("Failed to write to {}: {}", host, future.cause().toString());
      }
    }
  };
  private GraphiteClientChannelInitializer channelInitializer;
  private volatile ChannelFuture channelFuture;

  public NettyGraphiteClient(final MetricFactory metricFactory, final String host) {
    this.host = host;
    final String graphiteCompatibleHostName = HostName2MetricName.graphiteCompatibleHostPortName(host);
    errorCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".errors");
    pushBackCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".pushBack");
    reconnectCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".reconnect");
    rejectedCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".rejected");
    publishedCounter = metricFactory.createCounter(getClass().getSimpleName(), graphiteCompatibleHostName + ".published");
    log.info("Client for [{}] initialized", host);
  }

  public void setChannelInitializer(final GraphiteClientChannelInitializer channelInitializer) {
    this.channelInitializer = channelInitializer;
  }

  @Override
  public void connect() {
    reconnectCounter.inc();
    log.info("Client for [{}] is reconnecting", host);
    channelFuture = channelInitializer.connect();
  }

  @Override
  public boolean publishMetrics(final String metrics) {
    if (channelFuture.isDone()) {
      channelFuture.channel().writeAndFlush(metrics).addListener(opListener);
      return true;
    } else {
      rejectedCounter.inc();
      return false;
    }
  }

  @Override
  public void onPushBack() {
    pushBackCounter.inc();
  }

}
