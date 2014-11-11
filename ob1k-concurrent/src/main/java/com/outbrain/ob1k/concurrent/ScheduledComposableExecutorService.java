package com.outbrain.ob1k.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: aronen
 * Date: 10/2/13
 * Time: 1:25 AM
 */
public interface ScheduledComposableExecutorService extends ScheduledExecutorService {
  <T> ScheduledComposableFuture<T> schedule(Callable<T> callable, long delay, TimeUnit unit);
}
