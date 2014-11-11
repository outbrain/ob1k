package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.SyncRequestContext;

/**
 * Created by aronen on 6/9/14.
 *
 * a server side synchronous filter context.
 */
public interface SyncServerRequestContext extends ServerRequestContext, SyncRequestContext {
  SyncServerRequestContext nextPhase();
}
