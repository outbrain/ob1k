package com.outbrain.ob1k.concurrent.combiners;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 9/2/14.
 *
 * a generic two parameter method representation
 */
public interface BiFunction<T1, T2, R> {
  R apply(T1 left, T2 right) throws ExecutionException;
}
