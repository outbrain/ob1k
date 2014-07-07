package com.outbrain.gruffalo.netty;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.base.Preconditions;
import com.outbrain.swinfra.metrics.MetricFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Time: 8/4/13 2:20 PM
 *
 * @author Eran Harel
 */
public class GraphiteClientPool implements GraphiteClient {

  private static final Logger logger = LoggerFactory.getLogger(GraphiteClientPool.class);
  private final GraphiteClient[] pool;
  private AtomicInteger nextIndex = new AtomicInteger();

  public GraphiteClientPool(final String graphiteRelayHosts, final EventLoopGroup eventLoopGroup, final StringDecoder decoder,
      final StringEncoder encoder, final ChannelGroup activeServerChannels, final MetricFactory metricFactory) {
    Preconditions.checkNotNull(graphiteRelayHosts);
    Preconditions.checkNotNull(decoder, "decoder must not be null");
    Preconditions.checkNotNull(encoder, "encoder must not be null");
    Preconditions.checkNotNull(eventLoopGroup, "eventLoopGroup must not be null");

    logger.info("Creating a client pool for [{}]", graphiteRelayHosts);
    String[] hosts = graphiteRelayHosts.trim().split(",");
    pool = new GraphiteClient[hosts.length];
    initClients(hosts, eventLoopGroup, decoder, encoder, activeServerChannels, metricFactory);
  }

  private void initClients(String[] hosts, EventLoopGroup eventLoopGroup, StringDecoder decoder, StringEncoder encoder,
      ChannelGroup activeServerChannels, MetricFactory metricFactory) {
    for (int i = 0; i < hosts.length; i++) {
      String[] hostAndPort = hosts[i].split(":");
      String host = hostAndPort[0];
      int port = Integer.parseInt(hostAndPort[1]);

      final NettyGraphiteClient client = new NettyGraphiteClient(metricFactory, hosts[i]);
      pool[i] = client;
      ChannelHandler graphiteChannelHandler = new GraphiteChannelInboundHandler(client, hosts[i], activeServerChannels);
      final GraphiteClientChannelInitializer channelInitializer = new GraphiteClientChannelInitializer(host, port, eventLoopGroup, decoder, encoder,
          graphiteChannelHandler);
      client.setChannelInitializer(channelInitializer);
    }
  }

  @Override
  public void connect() {
    for (GraphiteClient client : pool) {
      client.connect();
    }
  }

  @Override
  public void publishMetrics(String metrics) {
    final int currIndex = nextIndex.getAndIncrement() % pool.length;
    pool[currIndex].publishMetrics(metrics);
  }

  @Override
  public void onPushBack() {
    // meh
    // in the future we may implement some weighted round robin using this input
    // currently this is only used by the children to add metrics
  }
}
