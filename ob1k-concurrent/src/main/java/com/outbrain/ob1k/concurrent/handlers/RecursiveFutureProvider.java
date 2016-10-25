package com.outbrain.ob1k.concurrent.handlers;

import com.google.common.base.Predicate;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * @author marenzon
 * @param <T> type of computation value
 */
public interface RecursiveFutureProvider<T> {

  ComposableFuture<T> provide();
  Predicate<T> createStopCriteria();
}
