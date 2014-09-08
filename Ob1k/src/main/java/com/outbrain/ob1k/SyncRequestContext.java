package com.outbrain.ob1k;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 6/9/14.
 *
 * a synchronous request context.
 */
public interface SyncRequestContext extends RequestContext {
  <T> T invokeSync() throws ExecutionException;
}
