package com.outbrain.ob1k.common.filters;

import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * filters asynchronous requests.
 *
 * User: aronen
 * Date: 6/27/13
 * Time: 5:04 PM
 */
public interface AsyncFilter<T, C extends AsyncRequestContext> extends ServiceFilter {
  ComposableFuture<T> handleAsync(C ctx);
}
