package com.outbrain.gruffalo.netty;

import java.util.concurrent.TimeUnit;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;

@ChannelHandler.Sharable
public class GraphiteChannelInboundHandler extends SimpleChannelInboundHandler<String> {

  private static final Logger log = LoggerFactory.getLogger(GraphiteChannelInboundHandler.class);
  private static final int RECONNECT_DELAY_SEC = 5;

  private final GraphiteClient client;

  private final String graphiteTarget;
  private final ChannelGroup activeServerChannels;
  private boolean serverReadEnabled = true;

  private final ChannelFutureListener restoreServerReads = new ChannelFutureListener() {
    @Override
    public void operationComplete(final ChannelFuture future) throws Exception {
      changeServerAutoRead(true);
    }
  };

  public GraphiteChannelInboundHandler(final GraphiteClient client, final String graphiteTarget, final ChannelGroup activeServerChannels) {
    this.client = Preconditions.checkNotNull(client, "client may not be null");
    this.graphiteTarget = graphiteTarget;
    this.activeServerChannels = Preconditions.checkNotNull(activeServerChannels, "activeServerChannels must not be null");
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
    log.warn("Got an unexpected downstream message: " + msg);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    log.info("Connected to: {}", ctx.channel().remoteAddress());
  }

  @Override
  public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
    log.warn("Got disconnected from {}... will try to reconnect in {} sec...", graphiteTarget, RECONNECT_DELAY_SEC);
    scheduleReconnect(ctx);
  }

  private void scheduleReconnect(final ChannelHandlerContext ctx) {
    final EventLoop loop = ctx.channel().eventLoop();
    loop.schedule(new Runnable() {
      @Override
      public void run() {
        log.info("Reconnecting to {}", graphiteTarget);
        client.connect();
      }
    }, RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
    if (evt instanceof IdleStateEvent) {
      final IdleStateEvent e = (IdleStateEvent) evt;
      if (!serverReadEnabled && e.state() == IdleState.WRITER_IDLE) {
        // close the channel so we try to reconnect, and restore reads
        log.info("Outbound connection to {} seem disconnected. Closing channel and restoring server reads.", graphiteTarget);
        ctx.channel().close().addListener(restoreServerReads);
      }
    }
  }

  @Override
  public void channelWritabilityChanged(final ChannelHandlerContext ctx) throws Exception {
    final boolean autoread = ctx.channel().isWritable();
    serverReadEnabled = autoread;
    if (!autoread) {
      client.onPushBack();
    }

    log.debug("Setting server autoread={}", autoread);
    changeServerAutoRead(autoread);
  }

  private void changeServerAutoRead(final boolean autoread) {
    boolean serverChannelChanged = false;
    for (final Channel activeServerChannel : activeServerChannels) {
      if (!serverChannelChanged) {
        activeServerChannel.parent().config().setAutoRead(autoread);
        serverChannelChanged = true;
      }
      activeServerChannel.config().setAutoRead(autoread);
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    log.error("Unexpected exception from downstream.", cause);
    ctx.close();
  }

}
