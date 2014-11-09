package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.server.registry.endpoints.SyncServerEndpoint;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 6/10/14.
 *
 * the actual sync context for the server side call chain.
 */
public class DefaultSyncServerRequestContext extends AbstractServerRequestContext<SyncServerEndpoint> implements SyncServerRequestContext {
  public DefaultSyncServerRequestContext(final Request request, final SyncServerEndpoint endpoint, final Object[] params) {
    super(request, endpoint, params);
  }

  private DefaultSyncServerRequestContext(final Request request, final SyncServerEndpoint endpoint, final Object[] params, final int executionIndex) {
    super(request, endpoint, params, executionIndex);
  }

  @Override
  public SyncServerRequestContext nextPhase() {
    return new DefaultSyncServerRequestContext(request, endpoint, params, executionIndex + 1);
  }

  @Override
  public <T> T invokeSync() throws ExecutionException {
    return endpoint.invokeSync(this);
  }

}
