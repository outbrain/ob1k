package com.outbrain.ob1k.server.cors;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsHandler;


/**
 * Custom CorsHandler to suppress IO exceptions.
 *
 * @author Doug Chimento &lt;dchimento@outbrain.com&gt;
 */
public final class CorsWrapperHandler extends CorsHandler {
  private static final Logger logger = LoggerFactory.getLogger(CorsWrapperHandler.class);

  public CorsWrapperHandler(final CorsConfig config) {
    super(convertConfig(config));
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
    if (cause instanceof IOException) {
      logger.debug("caught IO exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    } else {
      logger.warn("caught exception in handler; remote host={}", ctx.channel().remoteAddress(), cause);
    }
    ctx.close();
  }

  private static io.netty.handler.codec.http.cors.CorsConfig convertConfig(final CorsConfig corsConfig) {
    io.netty.handler.codec.http.cors.CorsConfig.Builder nettyBuilder;
    if (corsConfig.origins().isEmpty()) {
      nettyBuilder =  io.netty.handler.codec.http.cors.CorsConfig.withAnyOrigin();
    } else  {
      nettyBuilder = io.netty.handler.codec.http.cors.CorsConfig.withOrigins(corsConfig.origins().toArray(new String[]{}));
    }

    nettyBuilder.allowedRequestHeaders(corsConfig.allowedRequestHeaders().toArray(new String[]{}));
    nettyBuilder.exposeHeaders(corsConfig.exposedHeaders().toArray(new String[]{}));
    nettyBuilder.allowedRequestMethods(convertHeaders(corsConfig.allowedRequestMethods()));
    nettyBuilder.maxAge(corsConfig.maxAge());

    if (corsConfig.isCredentialsAllowed()) {
      nettyBuilder.allowCredentials();
    }
    if (corsConfig.isShortCircuit()) {
      nettyBuilder.shortCurcuit();
    }
    if (!corsConfig.isCorsSupportEnabled()) {
      nettyBuilder.disable();
    }
    if (corsConfig.isNullOriginAllowed()) {
      nettyBuilder.allowNullOrigin();
    }
    if (corsConfig.noPreflightHeaders()) {
      nettyBuilder.noPreflightResponseHeaders();
    }

    return nettyBuilder.build();
  }

  private static HttpMethod[] convertHeaders(final Set<String> sMethods) {
    HttpMethod[] methods = new HttpMethod[sMethods.size()];
    Iterator<String> iterator = sMethods.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      //TODO Handle Invalid Method?
      methods[i++] =  HttpMethod.valueOf(iterator.next());
    }
    return methods;
  }
}
