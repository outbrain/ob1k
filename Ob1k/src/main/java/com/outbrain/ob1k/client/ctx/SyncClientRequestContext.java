package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.SyncRequestContext;

/**
 * Created by aronen on 6/10/14.
 *
 * synchronous client side request context.
 */
public interface SyncClientRequestContext extends ClientRequestContext, SyncRequestContext {
  SyncClientRequestContext nextPhase();
}
