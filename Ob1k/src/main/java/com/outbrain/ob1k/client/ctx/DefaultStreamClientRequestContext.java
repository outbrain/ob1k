package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.endpoints.StreamClientEndpoint;
import rx.Observable;

/**
 * Created by aronen on 6/10/14.
 *
 * the default client side stream based request context.
 */
public class DefaultStreamClientRequestContext extends AbstractClientRequestContext implements StreamClientRequestContext {
  public DefaultStreamClientRequestContext(final String remoteTarget, final Object[] params, final AbstractClientEndpoint endpoint) {
    super(remoteTarget, params, endpoint);
  }

  private DefaultStreamClientRequestContext(final String remoteTarget, final Object[] params, final StreamClientEndpoint endpoint, final int executionIndex) {
    super(remoteTarget, params, endpoint, executionIndex);
  }

  @Override
  public StreamClientRequestContext nextPhase() {
    return new DefaultStreamClientRequestContext(remoteTarget, params, (StreamClientEndpoint) endpoint, executionIndex + 1);
  }

  @Override
  public <T> Observable<T> invokeStream() {
    return ((StreamClientEndpoint)endpoint).invokeStream(this);
  }
}
