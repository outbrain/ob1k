package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.server.registry.endpoints.StreamServerEndpoint;
import rx.Observable;

/**
 * Created by aronen on 6/10/14.
 *
 * the actual stream context for the server side call chain.
 */
public class DefaultStreamServerRequestContext extends AbstractServerRequestContext<StreamServerEndpoint> implements StreamServerRequestContext {
  public DefaultStreamServerRequestContext(final Request request, final StreamServerEndpoint endpoint, final Object[] params) {
    super(request, endpoint, params);
  }

  private DefaultStreamServerRequestContext(final Request request, final StreamServerEndpoint endpoint, final Object[] params, final int executionIndex) {
    super(request, endpoint, params, executionIndex);
  }

  @Override
  public StreamServerRequestContext nextPhase() {
    return new DefaultStreamServerRequestContext(request, endpoint, params, executionIndex + 1);
  }

  @Override
  public <T> Observable<T> invokeStream() {
    return endpoint.invokeStream(this);
  }
}
