package com.outbrain.ob1k.server.ctx;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.RequestContext;

/**
 * User: aronen
 * Date: 7/29/13
 * Time: 2:00 PM
 */
public interface ServerRequestContext extends RequestContext {
  Request getRequest();

  int getExecutionIndex();
}
