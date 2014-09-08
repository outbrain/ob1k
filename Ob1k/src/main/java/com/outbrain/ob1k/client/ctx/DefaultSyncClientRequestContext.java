package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.client.endpoints.SyncClientEndpoint;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 6/10/14.
 *
 * the default client side sync request context.
 */
public class DefaultSyncClientRequestContext extends AbstractClientRequestContext implements SyncClientRequestContext {
  public DefaultSyncClientRequestContext(final String remoteTarget, final Object[] params, final SyncClientEndpoint endpoint) {
    super(remoteTarget, params, endpoint);
  }

  private DefaultSyncClientRequestContext(final String remoteTarget, final Object[] params, final SyncClientEndpoint endpoint, final int executionIndex) {
    super(remoteTarget, params, endpoint, executionIndex);
  }

  @Override
  public SyncClientRequestContext nextPhase() {
    return new DefaultSyncClientRequestContext(remoteTarget, params, (SyncClientEndpoint) endpoint, executionIndex + 1);
  }

  @Override
  public <T> T invokeSync() throws ExecutionException {
    return ((SyncClientEndpoint)endpoint).invokeSync(this);
  }



}
