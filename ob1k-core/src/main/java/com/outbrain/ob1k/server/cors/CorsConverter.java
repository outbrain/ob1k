package com.outbrain.ob1k.server.cors;

import java.util.Iterator;
import java.util.Set;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cors.CorsHandler;

/**
 * Convert Outbrain Cors to {@link CorsHandler}.
 * @author Doug Chimento &lt;dchimento@outbrain.com&gt;
 */
public class CorsConverter {

  public static CorsHandler convertHandler(CorsConfig corsConfig) {
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

    return new CorsHandler(nettyBuilder.build());
  }

  private static HttpMethod[] convertHeaders(Set<String> sMethods) {
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
