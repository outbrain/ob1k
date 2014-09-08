package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.StreamRequestContext;

/**
 * Created by aronen on 6/10/14.
 *
 * stream based client side request context.
 */
public interface StreamClientRequestContext extends ClientRequestContext, StreamRequestContext {
  StreamClientRequestContext nextPhase();
}
