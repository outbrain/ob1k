package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 2/10/14.
 * used to calculate aggregated value of a set of chained asynchronous operation
 */
public interface ForeachHandler<T,R> {
  public ComposableFuture<R> handle(T element, R aggregateResult);
}
