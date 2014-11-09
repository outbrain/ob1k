package com.outbrain.ob1k;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 6/9/14.
 *
 * an asynchronous request context.
 */
public interface AsyncRequestContext extends RequestContext {
  <T> ComposableFuture<T> invokeAsync();
}
