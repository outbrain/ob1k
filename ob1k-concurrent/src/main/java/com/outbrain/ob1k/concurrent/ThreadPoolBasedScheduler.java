package com.outbrain.ob1k.concurrent;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * a scheduler based on java's ScheduledExecutorService.
 *
 * @author aronen
 */
public class ThreadPoolBasedScheduler implements Scheduler {
  private final ScheduledExecutorService scheduledThreadPool;

  public ThreadPoolBasedScheduler(final int numOfThreads) {
    this.scheduledThreadPool = Executors.newScheduledThreadPool(numOfThreads);
  }

  @Override
  public void schedule(final Runnable task, final long delay, final TimeUnit unit) {
    scheduledThreadPool.schedule(task, delay, unit);
  }

  @Override
  public void shutdown() {
    scheduledThreadPool.shutdown();
  }
}
