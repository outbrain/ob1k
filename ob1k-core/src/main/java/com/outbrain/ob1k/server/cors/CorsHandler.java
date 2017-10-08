package com.outbrain.ob1k.server.cors;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.cors.CorsConfig;

/**
 * Custom CorsHandler to suppress IO exceptions.
 *
 * @author Doug Chimento &lt;dchimento@outbrain.com&gt;
 */
public final class CorsHandler extends io.netty.handler.codec.http.cors.CorsHandler {
  private static final Logger logger = LoggerFactory.getLogger(CorsHandler.class);

  CorsHandler(CorsConfig config) {
    super(config);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      logger.debug("caught IO exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    } else {
      logger.warn("caught exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    }
    ctx.close();
  }
}
