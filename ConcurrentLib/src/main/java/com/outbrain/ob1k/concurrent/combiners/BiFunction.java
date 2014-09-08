package com.outbrain.ob1k.concurrent.combiners;

/**
 * Created by aronen on 9/2/14.
 */
public interface BiFunction<T1, T2, R> {
  R apply(T1 left, T2 right);
}
