package com.outbrain.ob1k;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 4/24/14.
 *
 * base interface for client and server request context.
 */
public interface RequestContext {
  Object[] getParams();
  String getServiceMethodName();
  String getServiceClassName();
}
