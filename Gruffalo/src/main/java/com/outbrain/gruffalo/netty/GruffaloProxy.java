package com.outbrain.gruffalo.netty;

import com.google.common.base.Preconditions;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

class GruffaloProxy {

  private static final Logger log = LoggerFactory.getLogger(GruffaloProxy.class);
  private final ChannelFuture tcpChannelFuture;
  private final ChannelFuture udpChannelFuture;
  private final EventLoopGroup eventLoopGroup;
  private final Throttler throttler;

  public GruffaloProxy(final EventLoopGroup eventLoopGroup, final TcpServerPipelineFactory tcpServerPipelineFactory,
                       final UdpServerPipelineFactory udpServerPipelineFactory, final int tcpPort, final int udpPort, final Throttler throttler) throws InterruptedException {
    this.throttler = Preconditions.checkNotNull(throttler, "throttler must not be null");
    this.eventLoopGroup = Preconditions.checkNotNull(eventLoopGroup, "eventLoopGroup must not be null");
    tcpChannelFuture = createTcpBootstrap(tcpServerPipelineFactory, tcpPort);
    udpChannelFuture = createUdpBootstrap(udpServerPipelineFactory, udpPort);
    log.info("Initialization completed");
  }

  public static void main(final String[] args) {
    new ClassPathXmlApplicationContext("classpath:applicationContext-GruffaloLib-all.xml");
  }

  private ChannelFuture createUdpBootstrap(final UdpServerPipelineFactory udpServerPipelineFactory, final int udpPort) throws InterruptedException {
    log.info("Initializing UDP...");
    Bootstrap udpBootstrap = new Bootstrap();
    udpBootstrap.group(eventLoopGroup).channel(NioDatagramChannel.class).handler(udpServerPipelineFactory);

    final ChannelFuture channelFuture = udpBootstrap.bind(udpPort);
    log.info("Binding to UDP port {}", udpPort);
    return channelFuture;
  }

  private ChannelFuture createTcpBootstrap(final TcpServerPipelineFactory tcpServerPipelineFactory, final int tcpPort) throws InterruptedException {
    log.info("Initializing TCP...");
    ServerBootstrap tcpBootstrap = new ServerBootstrap();
    tcpBootstrap.group(eventLoopGroup);
    tcpBootstrap.channel(NioServerSocketChannel.class);
    tcpBootstrap.childHandler(tcpServerPipelineFactory);
    tcpBootstrap.option(ChannelOption.ALLOCATOR, UnpooledByteBufAllocator.DEFAULT);

    final ChannelFuture channelFuture = tcpBootstrap.bind(tcpPort).addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(final ChannelFuture future) throws Exception {
        throttler.setServerChannel(future.channel());
      }
    });
    log.info("Binding to TCP port {}", tcpPort);
    return channelFuture;
  }

  public void shutdown() throws InterruptedException {
    log.info("Shutting down inbound channels...");
    tcpChannelFuture.sync().channel().closeFuture().await(200);
    udpChannelFuture.sync().channel().closeFuture().await(200);

    log.info("Shutting down server...");
    eventLoopGroup.shutdownGracefully();
  }
}
