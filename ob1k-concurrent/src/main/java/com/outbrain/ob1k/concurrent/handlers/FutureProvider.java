package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * A lazy iterator for futures.
 *
 * @author aronen
 */
public interface FutureProvider<T> {

  boolean moveNext();

  ComposableFuture<T> current();
}
