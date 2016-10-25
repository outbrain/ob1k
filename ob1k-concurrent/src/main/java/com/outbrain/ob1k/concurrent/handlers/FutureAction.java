package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * A functional interface for future based actions.
 *
 * @author aronen
 */
@FunctionalInterface
public interface FutureAction<T> {
  ComposableFuture<T> execute();
}

