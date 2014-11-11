package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 11/9/14.
 * a lazy iterator for futures.
 */
public interface FutureProvider<T> {
  boolean moveNext();
  ComposableFuture<T> current();
}

