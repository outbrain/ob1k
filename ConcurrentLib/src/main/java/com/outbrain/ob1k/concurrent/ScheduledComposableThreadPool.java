package com.outbrain.ob1k.concurrent;

import java.util.concurrent.*;

/**
 * User: aronen
 * Date: 10/2/13
 * Time: 12:29 AM
 */
public class ScheduledComposableThreadPool extends ScheduledThreadPoolExecutor implements ScheduledComposableExecutorService {
  public ScheduledComposableThreadPool(final int corePoolSize) {
    super(corePoolSize);
  }

  public ScheduledComposableThreadPool(final int corePoolSize, final ThreadFactory threadFactory) {
    super(corePoolSize, threadFactory);
  }

  public ScheduledComposableThreadPool(final int corePoolSize, final RejectedExecutionHandler handler) {
    super(corePoolSize, handler);
  }

  public ScheduledComposableThreadPool(final int corePoolSize, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
    super(corePoolSize, threadFactory, handler);
  }

  @Override
  protected <T> RunnableScheduledFuture<T> decorateTask(final Callable<T> callable, final RunnableScheduledFuture<T> task) {
    final long time = task.getDelay(TimeUnit.NANOSECONDS) + System.nanoTime();
    return new ScheduledSimpleComposableFuture<T>(callable, time);
  }

  @Override
  public <T> ScheduledComposableFuture<T> schedule(final Callable<T> callable, final long delay, final TimeUnit unit) {
    return (ScheduledComposableFuture<T>) super.schedule(callable, delay, unit);
  }
}
