package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.registry.endpoints.AsyncServerEndpoint;

/**
 * Created by aronen on 6/9/14.
 *
 * the actual async context for the server side call chain.
 */
public class DefaultAsyncServerRequestContext extends AbstractServerRequestContext implements AsyncServerRequestContext {
  public DefaultAsyncServerRequestContext(final Request request, final AsyncServerEndpoint endpoint, final Object[] params) {
    super(request, endpoint, params);
  }

  private DefaultAsyncServerRequestContext(final Request request, final AsyncServerEndpoint endpoint, final Object[] params, final int executionIndex) {
    super(request, endpoint, params, executionIndex);
  }

  @Override
  public AsyncServerRequestContext nextPhase() {
    return new DefaultAsyncServerRequestContext(request, (AsyncServerEndpoint) endpoint, params, executionIndex + 1);
  }

  @Override
  public <T> ComposableFuture<T> invokeAsync() {
    return ((AsyncServerEndpoint)endpoint).invokeAsync(this);
  }

}
