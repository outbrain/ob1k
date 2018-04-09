package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 11/11/14.
 * same as BiFunction but allow for returning a long running computation.
 */
public interface FutureBiFunction<T1, T2, R> {
  ComposableFuture<R> apply(T1 left, T2 right);
}
