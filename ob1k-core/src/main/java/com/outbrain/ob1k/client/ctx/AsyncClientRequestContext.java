package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.AsyncRequestContext;

/**
 * Created by aronen on 6/10/14.
 *
 * asynchronous client side request context.
 */
public interface AsyncClientRequestContext extends ClientRequestContext, AsyncRequestContext {
  AsyncClientRequestContext nextPhase();
}
