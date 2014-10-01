package com.outbrain.ob1k.concurrent.combiners;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 9/2/14.
 *
 * a generic three parameter method representation
 */
public interface TriFunction<T1, T2, T3, R> {
  R apply(T1 first, T2 second, T3 third) throws ExecutionException;
}
