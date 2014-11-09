package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.StreamRequestContext;

/**
 * Created by aronen on 6/9/14.
 * a server side context for stream based requests.
 */
public interface StreamServerRequestContext extends ServerRequestContext, StreamRequestContext {
  StreamServerRequestContext nextPhase();
}
