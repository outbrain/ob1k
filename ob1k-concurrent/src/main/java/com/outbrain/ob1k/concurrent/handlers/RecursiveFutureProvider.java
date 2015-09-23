package com.outbrain.ob1k.concurrent.handlers;

import com.google.common.base.Predicate;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * @author misha
 * @since 2015-09-23
 */
public interface RecursiveFutureProvider<T> {

  ComposableFuture<T> provide();
  Predicate<T> createStopCriteria();
}
