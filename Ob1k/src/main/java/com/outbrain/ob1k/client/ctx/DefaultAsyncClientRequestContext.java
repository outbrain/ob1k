package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.client.endpoints.AsyncClientEndpoint;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 6/10/14.
 *
 * the default client side async request context.
 */
public class DefaultAsyncClientRequestContext extends AbstractClientRequestContext implements AsyncClientRequestContext {
  public DefaultAsyncClientRequestContext(final String remoteTarget, final Object[] params, final AsyncClientEndpoint endpoint) {
    super(remoteTarget, params, endpoint);
  }

  private DefaultAsyncClientRequestContext(final String remoteTarget, final Object[] params, final AsyncClientEndpoint endpoint, final int executionIndex) {
    super(remoteTarget, params, endpoint, executionIndex);
  }

  @Override
  public AsyncClientRequestContext nextPhase() {
    return new DefaultAsyncClientRequestContext(remoteTarget, params, (AsyncClientEndpoint) endpoint, executionIndex + 1);
  }

  @Override
  public <T> ComposableFuture<T> invokeAsync() {
    return ((AsyncClientEndpoint)endpoint).invokeAsync(this);
  }


}
