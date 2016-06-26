package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;

/**
 * Created by aronen on 4/25/14.
 *
 * represent a single call on the client side.
 */
public abstract class AbstractClientRequestContext<Endpoint extends AbstractClientEndpoint> implements ClientRequestContext {

  protected final String remoteTarget;
  protected final Object[] params;
  protected final Endpoint clientEndpoint;
  protected final int executionIndex;
  protected final String url;

  public AbstractClientRequestContext(final String remoteTarget, final Object[] params, final Endpoint endpoint) {
    this(remoteTarget, params, endpoint, 0);
  }

  protected AbstractClientRequestContext(final String remoteTarget, final Object[] params, final Endpoint clientEndpoint,
                                         final int executionIndex) {
    this.remoteTarget = remoteTarget;
    this.params = params;
    this.clientEndpoint = clientEndpoint;
    this.executionIndex = executionIndex;
    this.url = createUrl(remoteTarget, clientEndpoint);
  }

  private static String createUrl(final String remoteTarget, final AbstractClientEndpoint clientEndpoint) {
    final String methodPath = clientEndpoint.getEndpointDescription().getMethodPath();
    return remoteTarget.endsWith("/") || methodPath.startsWith("/") ? remoteTarget + methodPath : remoteTarget + "/" + methodPath;
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
    return clientEndpoint.getEndpointDescription().getMethod().getName();
  }

  @Override
  public String getServiceClassName() {
    return clientEndpoint.getEndpointDescription().getServiceType().getSimpleName();
  }
}
