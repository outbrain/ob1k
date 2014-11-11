package com.outbrain.ob1k.client.ctx;

import com.outbrain.ob1k.RequestContext;

/**
 * Created by aronen on 4/24/14.
 *
 * the client side request context to be used inside a filter.
 */
public interface ClientRequestContext extends RequestContext {
  String getRemoteTarget();

  int getExecutionIndex();

  String getUrl();
}
