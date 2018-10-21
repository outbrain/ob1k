package com.outbrain.ob1k.client;



import com.outbrain.ob1k.client.dispatch.DispatchStrategy;
import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.endpoints.DispatchAction;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * @author aronen
 */
class HttpInvocationHandler implements InvocationHandler {

  private static final Logger logger = LoggerFactory.getLogger(HttpInvocationHandler.class);

  private final HttpClient client;
  private final Map<Method, AbstractClientEndpoint> endpoints;
  private final TargetProvider targetProvider;
  private final DispatchStrategy dispatchStrategy;

  HttpInvocationHandler(final TargetProvider targetProvider, final HttpClient client,
                        final Map<Method, AbstractClientEndpoint> endpoints, final DispatchStrategy dispatchStrategy) {

    this.client = Objects.requireNonNull(client, "client may not be null");
    this.targetProvider = Objects.requireNonNull(targetProvider, "targetProvider may not be null");
    this.endpoints = Objects.requireNonNull(endpoints, "endpoints may not be null");
    this.dispatchStrategy = Objects.requireNonNull(dispatchStrategy, "dispatchStrategy may not be null");
  }

  @Override
  public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {

    if (isCloseInvoke(method)) {
      client.close();
      logger.debug("client {} is closed.", targetProvider.getTargetLogicalName());
      return null;
    }

    final AbstractClientEndpoint endpoint = endpoints.get(method);
    final DispatchAction dispatchAction = endpoint.createDispatchAction(args);

    return endpoint.dispatch(targetProvider, dispatchStrategy, dispatchAction);
  }

  private boolean isCloseInvoke(final Method method) {
    return "close".equals(method.getName()) && method.getParameterTypes().length == 0;
  }
}
