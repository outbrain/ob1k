package com.outbrain.ob1k.client;

import static com.google.common.base.Preconditions.checkNotNull;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author aronen
 */
class HttpInvocationHandler implements InvocationHandler {

  private static final Logger logger = LoggerFactory.getLogger(HttpInvocationHandler.class);

  private final HttpClient client;
  private final Map<Method, AbstractClientEndpoint> endpoints;
  private final TargetProvider targetProvider;

  HttpInvocationHandler(final TargetProvider targetProvider, final HttpClient client, final Map<Method, AbstractClientEndpoint> endpoints) {

    this.client = checkNotNull(client, "client may not be null");
    this.targetProvider = checkNotNull(targetProvider, "targetProvider may not be null");
    this.endpoints = checkNotNull(endpoints, "endpoints may not be null");
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if ("close".equals(method.getName()) && method.getParameterTypes().length == 0) {

      client.close();
      if (logger.isDebugEnabled()) {
        logger.debug("client {} is closed.", targetProvider.getTargetLogicalName());
      }
      return null;
    }
    final AbstractClientEndpoint endpoint = endpoints.get(method);
    return endpoint.invoke(targetProvider, args);
  }
}
