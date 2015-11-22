package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpoint;

/**
 * User: aronen
 * Date: 7/29/13
 * Time: 2:08 PM
 */
public abstract class AbstractServerRequestContext<Endpoint extends ServerEndpoint> implements ServerRequestContext {
  protected final Request request;
  protected final Endpoint endpoint;
  protected final Object[] params;
  protected final int executionIndex;

  public AbstractServerRequestContext(final Request request, final Endpoint endpoint, final Object[] params) {
    this(request, endpoint, params, 0);
  }

  protected AbstractServerRequestContext(final Request request, final Endpoint endpoint, final Object[] params, final int executionIndex) {
    this.request = request;
    this.endpoint = endpoint;
    this.params = params;
    this.executionIndex = executionIndex;
  }

  @Override
  public Object[] getParams() {
    return params;
  }

  @Override
  public int getExecutionIndex() {
    return executionIndex;
  }

  @Override
  public Request getRequest() {
    return request;
  }

  @Override
  public String getServiceMethodName() {
    return endpoint.getMethod().getName();
  }

  @Override
  public String getServiceClassName() {
    return endpoint.getMethod().getDeclaringClass().getSimpleName();
  }
}
