package com.outbrain.ob1k.concurrent.combiners;

/**
 * Created by aronen on 9/2/14.
 */
public interface TriFunction<T1, T2, T3, R> {
  R apply(T1 first, T2 second, T3 third);
}
