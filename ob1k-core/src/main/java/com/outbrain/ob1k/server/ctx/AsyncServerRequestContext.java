package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.AsyncRequestContext;

/**
 * Created by aronen on 6/9/14.
 *
 * an common interface for asynchronous server side request context.
 */
public interface AsyncServerRequestContext extends ServerRequestContext, AsyncRequestContext {
  AsyncServerRequestContext nextPhase();
}
