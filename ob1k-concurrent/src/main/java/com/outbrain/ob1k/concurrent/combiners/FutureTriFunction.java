package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 11/11/14.
 * same as a TriFunction but allows for a long running computation to be returned.
 */
public interface FutureTriFunction<T1, T2, T3, R> {
  ComposableFuture<R> apply(T1 first, T2 second, T3 third);
}
