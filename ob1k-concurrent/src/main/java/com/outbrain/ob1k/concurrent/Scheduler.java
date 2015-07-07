package com.outbrain.ob1k.concurrent;


import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 1/8/15.
 *
 * a ComposableFuture based scheduler.
 */
public interface Scheduler {
  CancellationToken schedule(final Runnable task, final long delay, final TimeUnit timeUnit);
  void shutdown();
}