package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;

/**
 * Created by aronen on 4/25/14.
 *
 * represent a single call on the client side.
 */
public abstract class AbstractClientRequestContext implements ClientRequestContext {
  protected final String remoteTarget;
  protected final Object[] params;
  protected final AbstractClientEndpoint endpoint;
  protected final int executionIndex;
  protected final String url;

  public AbstractClientRequestContext(final String remoteTarget, final Object[] params, final AbstractClientEndpoint endpoint) {
    this(remoteTarget, params, endpoint, 0);
  }

  protected AbstractClientRequestContext(final String remoteTarget, final Object[] params, final AbstractClientEndpoint endpoint, final int executionIndex) {
    this.remoteTarget = remoteTarget;
    this.params = params;
    this.endpoint = endpoint;
    this.executionIndex = executionIndex;
    this.url = createUrl(remoteTarget, endpoint);
  }

  private static String createUrl(final String remoteTarget, final AbstractClientEndpoint endpoint) {
    final String methodPath = endpoint.methodPath;
    return remoteTarget.endsWith("/") ? remoteTarget + methodPath : remoteTarget + "/" + methodPath;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  public String getRemoteTarget() {
    return remoteTarget;
  }

  @Override
  public int getExecutionIndex() {
    return executionIndex;
  }

  @Override
  public Object[] getParams() {
    return params;
  }

  @Override
  public String getServiceMethodName() {
    return endpoint.method.getName();
  }

  @Override
  public String getServiceClassName() {
    return endpoint.serviceType.getSimpleName();
  }
}
