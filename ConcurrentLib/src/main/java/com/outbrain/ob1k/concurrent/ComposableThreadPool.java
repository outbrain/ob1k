package com.outbrain.ob1k.concurrent;

import java.util.concurrent.*;

/**
 * User: aronen
 * Date: 6/11/13
 * Time: 5:26 PM
 */
public class ComposableThreadPool extends ThreadPoolExecutor implements ComposableExecutorService {
  public ComposableThreadPool(final int coreSize, final int maxSize) {
    super(coreSize, maxSize, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    allowCoreThreadTimeOut(false);
  }

  public ComposableThreadPool(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime,
                              final TimeUnit unit, final BlockingQueue<Runnable> workQueue) {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
  }


    @Override
  protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
    return new SimpleComposableFuture<>(callable);
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(final Runnable runnable, final T value) {
    return new SimpleComposableFuture<>(runnable, value);
  }

  @Override
  public <T> ComposableFuture<T> submit(final Callable<T> task) {
    return (ComposableFuture<T>) super.submit(task);
  }

  @Override
  public <T> ComposableFuture<T> submit(final Callable<T> task, final boolean delegateHandler) {
    if (task == null)
      throw new NullPointerException();

    final SimpleComposableFuture<T> future = delegateHandler ?
        new SimpleComposableFuture<>(task, this) :
        new SimpleComposableFuture<>(task);

    execute(future);
    return future;
  }
}
