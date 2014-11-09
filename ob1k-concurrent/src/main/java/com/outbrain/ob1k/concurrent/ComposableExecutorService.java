package com.outbrain.ob1k.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * User: aronen
 * Date: 6/11/13
 * Time: 5:57 PM
 */
public interface ComposableExecutorService extends ExecutorService {
  <T> ComposableFuture<T> submit(Callable<T> task);
  <T> ComposableFuture<T> submit(Callable<T> task, boolean delegateHandler);
}
