package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.outbrain.ob1k.concurrent.combiners.*;
import com.outbrain.ob1k.concurrent.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.*;
import rx.Observable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 7:07 PM
 */
public class ComposableFutures {
  private ComposableFutures() {}

  private static class ExecutorServiceHolder {
    private static final ComposableExecutorService INSTANCE =
        new ComposableThreadPool(ComposableFutureConfig.corePoolSize, ComposableFutureConfig.maxOPoolSize);
  }

  private static class SchedulerServiceHolder {
    private static final ScheduledComposableExecutorService INSTANCE =
        new ScheduledComposableThreadPool(ComposableFutureConfig.timerSize);
  }

  private static Logger logger = LoggerFactory.getLogger(ComposableFutures.class);

  public static <T> ComposableFuture<T> recursive(final Supplier<ComposableFuture<T>> creator, final Predicate<T> stopCriteria) {
    return creator.get().continueOnSuccess(new FutureSuccessHandler<T, T>() {
      @Override
      public ComposableFuture<T> handle(final T result) {
        if (stopCriteria.apply(result)) {
          return ComposableFutures.fromValue(result);
        }
        return recursive(creator, stopCriteria);
      }
    });
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2) {
    return all(false, Arrays.asList(f1, f2));
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2, final ComposableFuture<T> f3) {
    return all(false, Arrays.asList(f1, f2, f3));
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2, final ComposableFuture<T> f3, final ComposableFuture<T> f4) {
    return all(false, Arrays.asList(f1, f2, f3, f4));
  }

  public static <T> ComposableFuture<List<T>> all(final Iterable<ComposableFuture<T>> futures) {
    return all(false, futures);
  }

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final Iterable<ComposableFuture<T>> futures) {
    return Combiner.all(failOnError, futures);
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError, final Map<K, ComposableFuture<T>> futures) {
    return Combiner.all(failOnError, futures);
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right,
                                                        final BiFunction<T1, T2, R> combiner) {
    return Combiner.combine(left, right, combiner);
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right,
                                                        final FutureBiFunction<T1, T2, R> combiner) {
    return Combiner.combine(left, right, combiner);
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first, final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third, final TriFunction<T1, T2, T3, R> combiner) {
    return Combiner.combine(first, second, third, combiner);
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first, final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third, final FutureTriFunction<T1, T2, T3, R> combiner) {
    return Combiner.combine(first, second, third, combiner);
  }

  public static <T> ComposableFuture<T> any(final ComposableFuture<T> f1, final ComposableFuture<T> f2) {
    return any(Arrays.asList(f1, f2));
  }

  public static <T> ComposableFuture<T> any(final ComposableFuture<T> f1, final ComposableFuture<T> f2, final ComposableFuture<T> f3) {
    return any(Arrays.asList(f1, f2, f3));
  }

  public static <T> ComposableFuture<T> any(final List<ComposableFuture<T>> futures) {
    final int size = futures.size();
    if (size == 0) {
      return fromError(new IllegalArgumentException("empty future list"));
    }

    final ComposablePromise<T> res = newPromise();
    final AtomicBoolean done = new AtomicBoolean();

    for (final ComposableFuture<T> future : futures) {
      future.onResult(new OnResultHandler<T>() {
        @Override
        public void handle(final ComposableFuture<T> result) {
          if (done.compareAndSet(false, true)) {
            try {
              res.set(result.get());
            } catch (final Exception e) {
              res.setException(e);
            }
          }
        }
      });
    }

    return res;
  }

  public static <T, R> ComposableFuture<R> foreach(final List<T> elements, final R zero, final ForeachHandler<T, R> handler) {
    ComposableFuture<R> result = fromValue(zero);
    for (final T element : elements) {
      result = result.continueOnSuccess(new FutureSuccessHandler<R, R>() {
        @Override
        public ComposableFuture<R> handle(final R result) {
          return handler.handle(element, result);
        }
      });
    }
    return result;
  }

  public static <R> ComposableFuture<R> repeat(final int iterations, final R zero, final FutureSuccessHandler<R, R> handler) {
    ComposableFuture<R> result = fromValue(zero);
    for (int i = 0; i < iterations; ++i) {
      result = result.continueOnSuccess(new FutureSuccessHandler<R, R>() {
        @Override
        public ComposableFuture<R> handle(final R result) {
          return handler.handle(result);
        }
      });
    }
    return result;
  }

  public static <T> ComposableFuture<T> fromValue(final T value) {
    return new FulfilledFuture<>(value);
  }

  public static <T> ComposableFuture<T> fromError(final Throwable error) {
    return new FulfilledFuture<>(error);
  }

  public static <T> ComposableFuture<T> fromNull() {
    return fromValue(null);
  }

  public static <T> ComposableFuture<T> submitFuture(final Callable<ComposableFuture<T>> task) {
    final ComposableFuture<ComposableFuture<T>> submitRes = submit(false, task);
    return submitRes.continueOnSuccess(new FutureSuccessHandler<ComposableFuture<T>, T>() {
      @Override
      public ComposableFuture<T> handle(ComposableFuture<T> result) {
        return result;
      }
    });
  }

  public static <T> ComposableFuture<T> submit(final Callable<T> task) {
    return submit(false, task);
  }

  public static <T> ComposableFuture<T> from(final Callable<T> task) {
    return submit(false, task);
  }

  public static <T> ComposableFuture<T> from(final boolean delegateHandler, final Callable<T> task) {
    return submit(delegateHandler, task);
  }

  public static <T> ComposableFuture<T> submit(final boolean delegateHandler, final Callable<T> task) {
    return ExecutorServiceHolder.INSTANCE.submit(task, delegateHandler);
  }

  public static <T> ComposableFuture<T> from(final ComposableExecutorService executor, final Callable<T> task) {
    return executor.submit(task, true);
  }

  public static <T, S> ComposableFuture<S> from(final T value, final Function<? super T, ? extends S> function) {
    return from(new Callable<S>() {
      @Override
      public S call() throws Exception {
        return function.apply(value);
      }
    });
  }

  public static <T> ScheduledComposableFuture<T> schedule(final Callable<T> task, final long delay, final TimeUnit unit) {
    return SchedulerServiceHolder.INSTANCE.schedule(task, delay, unit);
  }

  public static <T> ComposableFuture<T> scheduleFuture(final Callable<ComposableFuture<T>> task, final long delay, final TimeUnit unit) {
    final ScheduledComposableFuture<ComposableFuture<T>> schedule = schedule(task, delay, unit);
    return schedule.continueOnSuccess(new FutureSuccessHandler<ComposableFuture<T>, T>() {
      @Override
      public ComposableFuture<T> handle(ComposableFuture<T> result) {
        return result;
      }
    });
  }

  public static <T> ComposablePromise<T> newPromise() {
    return newPromise(false);
  }

  public static <T> ComposablePromise<T> newPromise(final ComposableExecutorService executor) {
    return new SimpleComposableFuture<>(executor);
  }

  public static <T> ComposablePromise<T> newPromise(final boolean delegateHandler) {
    if (delegateHandler) {
      return new SimpleComposableFuture<>(ExecutorServiceHolder.INSTANCE);
    } else {
      return new SimpleComposableFuture<>();
    }
  }

  public static <T> ComposablePromise<T> withTimeout(final ComposablePromise<T> future, final long duration, final TimeUnit unit) {
    SchedulerServiceHolder.INSTANCE.schedule(new Runnable() {
      @Override
      public void run() {
        if (!future.isDone()) {
          future.setException(new TimeoutException("timeout occurred on future(" + duration + " " + unit + ")"));
        }
      }
    }, duration, unit);

    return future;
  }

  public static <T> ComposableFuture<T> retry(final int retries, final FutureAction<T> action) {
    return action.execute().continueOnError(new FutureErrorHandler<T>() {
      @Override
      public ComposableFuture<T> handle(final Throwable error) {
        if (retries < 1)
          return ComposableFutures.fromError(error);
        else
          return retry(retries - 1, action);
      }
    });
  }

  public static <T> ComposableFuture<T> retry(final int retries, final long duration, final TimeUnit unit, final FutureAction<T> action) {
    return action.execute().withTimeout(duration, unit).continueOnError(new FutureErrorHandler<T>() {
      @Override
      public ComposableFuture<T> handle(final Throwable error) {
        if (retries < 1)
          return ComposableFutures.fromError(error);
        else
          return retry(retries - 1, action);
      }
    });
  }

  public static <T> rx.Observable<T> toObservable(final List<ComposableFuture<T>> futures) {
    return toObservable(futures, true);
  }

  public static <T> Observable<T> toObservable(final List<ComposableFuture<T>> futures, final boolean failOnError) {
    final FuturesToStreamHandler<T> handler = new FuturesToStreamHandler<>(futures, failOnError);
    return Observable.create(handler);
  }

  public static <T> Observable<T> toObservable(final FutureProvider<T> provider) {
    return Observable.create(new FutureProviderToStreamHandler<>(provider));
  }

//  public static <K, V> Observable<Map.Entry<K, V>> toObservable(final List<K> keys, final int bulkSize, final FutureBulkProvider<K, V> provider) {
//    return toObservable(new FutureProvider<Map.Entry<K, V>>() {
//      @Override
//      public boolean moveNext() {
//        ???
//        return false;
//      }
//
//      @Override
//      public ComposableFuture<Map.Entry<K, V>> current() {
//        return null;
//      }
//    });
//  }
//
//  public static interface FutureBulkProvider<K, V> {
//    public ComposableFuture<Map<K, V>> getBulk(List<K> keys);
//  }

  public static <T> Observable<T> toObservable(final FutureAction<T> provider, final int iterations) {
    return toObservable(new FutureProvider<T>() {
      volatile int iteration = iterations;
      @Override
      public boolean moveNext() {
        iteration -= 1;
        return iteration < 0;
      }

      @Override
      public ComposableFuture<T> current() {
        return provider.execute();
      }
    });
  }


  public static ExecutorService getExecutor() {
    return ExecutorServiceHolder.INSTANCE;
  }
}
