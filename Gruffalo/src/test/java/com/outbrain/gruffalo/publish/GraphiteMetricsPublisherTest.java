package com.outbrain.gruffalo.publish;

import com.google.common.base.Stopwatch;
import com.outbrain.gruffalo.netty.GraphiteChannelInboundHandler;
import com.outbrain.gruffalo.netty.GraphiteClientChannelInitializer;
import com.outbrain.gruffalo.netty.NettyGraphiteClient;
import com.outbrain.metrics.MetricFactory;
import com.yammer.metrics.core.Counter;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

/**
 * Time: 7/29/13 11:27 AM
 *
 * @author Eran Harel
 */
public class GraphiteMetricsPublisherTest {

  public static void main(String[] args) throws InterruptedException {

    final MetricFactory metricFactoryMock = Mockito.mock(MetricFactory.class);
    final Counter counterMock = Mockito.mock(Counter.class);
    Mockito.when(metricFactoryMock.createCounter(Mockito.anyString(), Mockito.anyString())).thenReturn(counterMock);

    NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(2);
    NettyGraphiteClient client = new NettyGraphiteClient(metricFactoryMock, "localhost:666");
    String host = "localhost";
    int port = 3003;
    GraphiteClientChannelInitializer channelInitializer = new GraphiteClientChannelInitializer(host, port, eventLoopGroup, new StringDecoder(), new StringEncoder(), new GraphiteChannelInboundHandler(client, host + ":" + port, new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)));
    client.setChannelInitializer(channelInitializer);
    client.connect();

//    Thread.sleep(20000);
    System.out.println("Begin bombardment...");
    Stopwatch time = new Stopwatch();
    time.start();
    for (int i = 0; i < 10000000; i++) {
      client.publishMetrics(i + " - 0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789\n");
      if(i % 10000 == 0) {
        Thread.sleep(100);
      }
      if (i % 100000 == 0) {
        System.out.println(i);
        Thread.sleep(300);
      }
    }
    time.stop();

    System.out.println("sent all data: " + time +"; shutting down...");

    Thread.sleep(1000000);
    eventLoopGroup.shutdownGracefully(10, 20, TimeUnit.SECONDS);
  }

}
