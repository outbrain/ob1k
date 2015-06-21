package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.outbrain.ob1k.concurrent.combiners.*;
import com.outbrain.ob1k.concurrent.config.Configuration;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;
import com.outbrain.ob1k.concurrent.eager.EagerComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.*;
import com.outbrain.ob1k.concurrent.lazy.LazyComposableFuture;
import com.outbrain.ob1k.concurrent.stream.FutureProviderToStreamHandler;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;

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
    private ComposableFutures() {
    }

    private static class ExecutorServiceHolder {
        private static final ExecutorService INSTANCE =
            createExecutor(Configuration.getExecutorCoreSize(), Configuration.getExecutorMaxSize());

        private static ExecutorService createExecutor(final int coreSize, final int maxSize) {
            final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(coreSize, maxSize,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
              new PrefixBasedThreadFactory("ob1k-main"));
            threadPool.allowCoreThreadTimeOut(false);

            return threadPool;
        }
    }

    private static class SchedulerServiceHolder {
        private static final Scheduler INSTANCE =
            new ThreadPoolBasedScheduler(Configuration.getSchedulerCoreSize(),
              new PrefixBasedThreadFactory("ob1k-scheduler-service").withDaemonThreads());

    }

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

    public static <K, T> ComposableFuture<Map<K, T>> first(final Map<K, ComposableFuture<T>> futures, final int numOfSuccess) {
        return Combiner.first(futures, numOfSuccess, false, null, null);
    }

    public static <K, T> ComposableFuture<Map<K, T>> first(final Map<K, ComposableFuture<T>> futures,
                                                           final int numOfSuccess, final long timeout, final TimeUnit timeUnit) {
        return Combiner.first(futures, numOfSuccess, false, timeout, timeUnit);
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
        return Combiner.any(futures);
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
        return fromValueEager(value);
    }

    public static <T> ComposableFuture<T> fromValueEager(final T value) {
        return EagerComposableFuture.fromValue(value);
    }

    public static <T> ComposableFuture<T> fromValueLazy(final T value) {
        return LazyComposableFuture.fromValue(value);
    }

    public static <T> ComposableFuture<T> fromError(final Throwable error) {
        return fromErrorEager(error);
    }

    public static <T> ComposableFuture<T> fromErrorEager(final Throwable error) {
        return EagerComposableFuture.fromError(error);
    }

    public static <T> ComposableFuture<T> fromErrorLazy(final Throwable error) {
        return LazyComposableFuture.fromError(error);
    }

    public static <T> ComposableFuture<T> fromTry(final Try<T> tryValue) {
        if (tryValue.isSuccess()) {
            return fromValue(tryValue.getValue());
        } else {
            return fromError(tryValue.getError());
        }
    }

    public static <T> ComposableFuture<T> fromNull() {
        return fromValue(null);
    }

    public static <T> ComposableFuture<T> submitFuture(final Callable<ComposableFuture<T>> task) {
        final ComposableFuture<ComposableFuture<T>> submitRes = submit(false, task);
        return submitRes.continueOnSuccess(new FutureSuccessHandler<ComposableFuture<T>, T>() {
            @Override
            public ComposableFuture<T> handle(final ComposableFuture<T> result) {
                return result;
            }
        });
    }

    /**
     * sends a callable task to the default thread pool and returns a ComposableFuture that represent the result.
     *
     * @param task the task to run.
     * @param <T>  the future type
     * @return a future representing the result.
     */
    public static <T> ComposableFuture<T> submit(final Callable<T> task) {
        return submit(false, task);
    }

    public static <T> ComposableFuture<T> submit(final Executor executor, final Callable<T> task) {
        return EagerComposableFuture.submit(executor, task, false);
    }

    public static <T> ComposableFuture<T> submit(final boolean delegateHandler, final Callable<T> task) {
        return submitEager(delegateHandler, task);
    }

    public static <T> ComposableFuture<T> submitEager(final boolean delegateHandler, final Callable<T> task) {
        return EagerComposableFuture.submit(ExecutorServiceHolder.INSTANCE, task, delegateHandler);
    }

    public static <T> ComposableFuture<T> submitLazy(final boolean delegateHandler, final Callable<T> task) {
        return LazyComposableFuture.submit(ExecutorServiceHolder.INSTANCE, task, delegateHandler);
    }

    public static <T, S> ComposableFuture<S> from(final T value, final Function<? super T, ? extends S> function) {
        return submit(new Callable<S>() {
            @Override
            public S call() throws Exception {
                return function.apply(value);
            }
        });
    }

    public static <T> ComposableFuture<T> schedule(final Callable<T> task, final long delay, final TimeUnit unit) {
        return scheduleEager(task, delay, unit);
    }

    public static <T> ComposableFuture<T> scheduleLazy(final Callable<T> task, final long delay, final TimeUnit unit) {
        return LazyComposableFuture.schedule(SchedulerServiceHolder.INSTANCE, task, delay, unit);
    }

    public static <T> ComposableFuture<T> scheduleEager(final Callable<T> task, final long delay, final TimeUnit unit) {
        return EagerComposableFuture.schedule(SchedulerServiceHolder.INSTANCE, task, delay, unit);
    }

    public static <T> ComposableFuture<T> scheduleFuture(final Callable<ComposableFuture<T>> task, final long delay, final TimeUnit unit) {
        final ComposableFuture<ComposableFuture<T>> schedule = schedule(task, delay, unit);
        return schedule.continueOnSuccess(new FutureSuccessHandler<ComposableFuture<T>, T>() {
            @Override
            public ComposableFuture<T> handle(final ComposableFuture<T> result) {
                return result;
            }
        });
    }

    /**
     * creates a new Promise. the promise can be used to create a single eager future.
     *
     * @param <T> the future type.
     * @return a promise
     */
    public static <T> ComposablePromise<T> newPromise() {
        return newPromise(false);
    }

    public static <T> ComposablePromise<T> newPromise(final Executor executor) {
        return new EagerComposableFuture<>(executor);
    }

    public static <T> ComposablePromise<T> newPromise(final boolean delegateHandler) {
        if (delegateHandler) {
            return new EagerComposableFuture<>(ExecutorServiceHolder.INSTANCE);
        } else {
            return new EagerComposableFuture<>();
        }
    }

    public static <T> ComposableFuture<T> build(final Producer<T> producer) {
        return buildEager(producer);
    }

    /**
     * builds a lazy future from a producer. the producer itself is cached
     * and used afresh on every consumption.
     *
     * @param producer the result producer
     * @param <T>      the future type
     * @return the future
     */
    public static <T> ComposableFuture<T> buildLazy(final Producer<T> producer) {
        return LazyComposableFuture.build(producer);
    }

    /**
     * builds a new eager future from a producer. the producer is consumed only once
     * abd the result(or error) is cached for future consumption.
     *
     * @param producer the result producer
     * @param <T>      the future type
     * @return the future ;)
     */
    public static <T> ComposableFuture<T> buildEager(final Producer<T> producer) {
        return EagerComposableFuture.build(producer);
    }

    /**
     * adds a time cap to the provided future.
     * if response do not arrive after the specified time a TimeoutException is returned from the returned future.
     *
     * @param future   the source future
     * @param duration time duration before emitting a timeout
     * @param unit     the duration time unit
     * @param <T>      the future type
     * @return a new future with a timeout
     */
    public static <T> ComposableFuture<T> withTimeout(final ComposableFuture<T> future, final long duration, final TimeUnit unit) {
        return future.withTimeout(SchedulerServiceHolder.INSTANCE, duration, unit);
    }

    /**
     * reties an eager future on failure "retries" times.
     *
     * @param retries max amount of retries
     * @param action  the eager future provider
     * @param <T>     the future type
     * @return the composed result.
     */
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

    /**
     * reties an eager future on failure "retries" times. each try is time capped with the specified time limit.
     *
     * @param retries  max amount of retries
     * @param duration the max time duration allowed for each try
     * @param unit     the duration time unit
     * @param action   the eager future provider
     * @param <T>      the future type
     * @return the composed result.
     */
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

    /**
     * retries a lazy future on failure "retries" times.
     *
     * @param future  the lazy future
     * @param retries max amount of reties
     * @param <T>     the future type
     * @return the composed result.
     */
    public static <T> ComposableFuture<T> retryLazy(final ComposableFuture<T> future, final int retries) {
        return future.continueOnError(new FutureErrorHandler<T>() {
            @Override
            public ComposableFuture<T> handle(final Throwable error) {
                if (retries < 1)
                    return ComposableFutures.fromError(error);
                else
                    return retryLazy(future, retries - 1);
            }
        });
    }

    public static <T> ComposableFuture<T> retryLazy(final ComposableFuture<T> future, final int retries, final long duration, final TimeUnit unit) {
        return future.withTimeout(duration, unit).continueOnError(new FutureErrorHandler<T>() {
            @Override
            public ComposableFuture<T> handle(final Throwable error) {
                if (retries < 1)
                    return ComposableFutures.fromError(error);
                else
                    return retryLazy(future, retries - 1, duration, unit);
            }
        });
    }

    /**
     * creates a future that fires the first future immediately and a second one after a specified time period
     * if result hasn't arrived yet.
     * should be used with eager futures.
     *
     * @param duration time to wait until the second future is fired
     * @param unit     the duration time unit
     * @param action   a provider of eager future
     * @param <T>      the type of the future
     * @return the composed future
     */
    public static <T> ComposableFuture<T> doubleDispatch(final long duration, final TimeUnit unit, final FutureAction<T> action) {
        return EagerComposableFuture.doubleDispatch(action, duration, unit, getScheduler());
    }

    /**
     * creates a future that fires the first future immediately (after consumption) and a second one after a specified time period
     * if result hasn't arrived yet.
     * can only be used with lazy futures.
     *
     * @param future   the original lazy future
     * @param duration time duration before consuming the future the second time
     * @param unit     th4e duration time unit.
     * @param <T>      the future type
     * @return the composed future
     */
    public static <T> ComposableFuture<T> doubleDispatch(final ComposableFuture<T> future, final long duration, final TimeUnit unit) {
        return ((LazyComposableFuture<T>) future).doubleDispatch(getScheduler(), duration, unit);
    }

    public static <T> rx.Observable<T> toColdObservable(final List<ComposableFuture<T>> futures) {
        return toColdObservable(futures, true);
    }

    /**
     * translate a list of lazy futures to a cold Observable stream
     *
     * @param futures     the lazy list of futures
     * @param failOnError whether to close the stream upon a future error
     * @param <T>         the stream type
     * @return the stream
     */
    public static <T> Observable<T> toColdObservable(final List<ComposableFuture<T>> futures, final boolean failOnError) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                final AtomicInteger counter = new AtomicInteger(futures.size());
                final AtomicBoolean errorTrigger = new AtomicBoolean(false);

                for (final ComposableFuture<T> future : futures) {
                    future.consume(new Consumer<T>() {
                        @Override
                        public void consume(final Try<T> result) {
                            if (result.isSuccess()) {
                                subscriber.onNext(result.getValue());
                                if (counter.decrementAndGet() == 0) {
                                    subscriber.onCompleted();
                                }
                            } else {
                                if (failOnError) {
                                    if (errorTrigger.compareAndSet(false, true)) {
                                        subscriber.onError(result.getError());
                                    }
                                    counter.set(0);
                                } else {
                                    if (counter.decrementAndGet() == 0) {
                                        subscriber.onCompleted();
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * translate a list of eager futures into a hot Observable stream
     * the results of the futures will be stored in the stream for any future subscriber.
     *
     * @param futures     the list of eager futures
     * @param failOnError whether to close the stream upon a future error
     * @param <T>         the stream type
     * @return the stream
     */
    public static <T> Observable<T> toHotObservable(final List<ComposableFuture<T>> futures, final boolean failOnError) {
        final ReplaySubject<T> subject = ReplaySubject.create(futures.size());
        final AtomicInteger counter = new AtomicInteger(futures.size());
        final AtomicBoolean errorTrigger = new AtomicBoolean(false);

        for (final ComposableFuture<T> future : futures) {
            future.consume(new Consumer<T>() {
                @Override
                public void consume(final Try<T> result) {
                    if (result.isSuccess()) {
                        subject.onNext(result.getValue());
                        if (counter.decrementAndGet() == 0) {
                            subject.onCompleted();
                        }
                    } else {
                        if (failOnError) {
                            if (errorTrigger.compareAndSet(false, true)) {
                                subject.onError(result.getError());
                            }
                            counter.set(0);
                        } else {
                            if (counter.decrementAndGet() == 0) {
                                subject.onCompleted();
                            }
                        }
                    }
                }
            });
        }

        return subject;
    }

    public static <T> Observable<T> toObservable(final FutureProvider<T> provider) {
        return Observable.create(new FutureProviderToStreamHandler<>(provider));
    }

    public static ExecutorService getExecutor() {
        return ExecutorServiceHolder.INSTANCE;
    }

    public static Scheduler getScheduler() {
        return SchedulerServiceHolder.INSTANCE;
    }
}
