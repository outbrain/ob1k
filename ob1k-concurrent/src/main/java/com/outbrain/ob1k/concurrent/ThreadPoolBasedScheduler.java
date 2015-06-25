package com.outbrain.ob1k.concurrent;

import java.util.concurrent.*;

/**
 * a scheduler based on java's ScheduledExecutorService.
 *
 * @author aronen
 */
public class ThreadPoolBasedScheduler implements Scheduler {
  private final ScheduledExecutorService scheduledThreadPool;

  public ThreadPoolBasedScheduler(final int numOfThreads,String threadNamePrefix) {
    this.scheduledThreadPool = Executors.newScheduledThreadPool(numOfThreads,
      new PrefixBasedThreadFactory(threadNamePrefix));
  }
  public ThreadPoolBasedScheduler(final int numOfThreads,ThreadFactory threadFactory) {
    this.scheduledThreadPool = Executors.newScheduledThreadPool(numOfThreads,threadFactory);
  }

  @Override
  public CancellationToken schedule(final Runnable task, final long delay, final TimeUnit unit) {
    return  new ScheduledFutureCancellationToken(scheduledThreadPool.schedule(task, delay, unit));
  }

  @Override
  public void shutdown() {
    scheduledThreadPool.shutdown();
  }
}
