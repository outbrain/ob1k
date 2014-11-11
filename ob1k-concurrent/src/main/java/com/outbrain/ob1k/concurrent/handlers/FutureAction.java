package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 5/29/14.
 *
 * a functional interface for future based actions.
 */
public interface FutureAction<T> {
  ComposableFuture<T> execute();
}

