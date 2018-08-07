package com.outbrain.ob1k.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.outbrain.ob1k.concurrent.combiners.Combiner;
import com.outbrain.ob1k.concurrent.combiners.FutureBiFunction;
import com.outbrain.ob1k.concurrent.combiners.FutureTriFunction;
import com.outbrain.ob1k.concurrent.combiners.TriFunction;
import com.outbrain.ob1k.concurrent.config.Configuration;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;
import com.outbrain.ob1k.concurrent.eager.EagerComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.ForeachHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureAction;
import com.outbrain.ob1k.concurrent.handlers.FutureProvider;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.RecursiveFutureProvider;
import com.outbrain.ob1k.concurrent.lazy.LazyComposableFuture;
import com.outbrain.ob1k.concurrent.stream.FutureProviderToStreamHandler;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * A set of helpers for ComposableFuture
 *
 * @author aronen
 */
public class ComposableFutures {

  private ComposableFutures() {
  }

  public static <T> ComposableFuture<T> recursive(final Supplier<ComposableFuture<T>> creator,
                                                  final Predicate<T> stopCriteria) {
    return creator.get().flatMap(result -> {
      if (stopCriteria.apply(result)) {
        return ComposableFutures.fromValue(result);
      }
      return recursive(creator, stopCriteria);
    });
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2) {
    return all(false, Arrays.asList(f1, f2));
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2,
                                                  final ComposableFuture<T> f3) {
    return all(false, Arrays.asList(f1, f2, f3));
  }

  public static <T> ComposableFuture<List<T>> all(final ComposableFuture<T> f1, final ComposableFuture<T> f2,
                                                  final ComposableFuture<T> f3, final ComposableFuture<T> f4) {
    return all(false, Arrays.asList(f1, f2, f3, f4));
  }

  public static <T> ComposableFuture<List<T>> all(final Iterable<ComposableFuture<T>> futures) {
    return all(false, futures);
  }

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError,
                                                  final Iterable<ComposableFuture<T>> futures) {
    return Combiner.all(failOnError, futures);
  }

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final Iterable<ComposableFuture<T>> elements, final Long timeout, final TimeUnit timeUnit) {
    return Combiner.all(failOnError, elements, timeout, timeUnit);
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError,
                                                       final Map<K, ComposableFuture<T>> futures) {
    return Combiner.all(failOnError, futures);
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError, final Map<K, ComposableFuture<T>> futures, final Long timeout, final TimeUnit timeUnit) {
    return Combiner.all(failOnError, futures, timeout, timeUnit);
  }

  public static <K, T> ComposableFuture<Map<K, T>> first(final Map<K, ComposableFuture<T>> futures,
                                                         final int numOfSuccess) {
    return Combiner.first(futures, numOfSuccess, false, null, null);
  }

  public static <K, T> ComposableFuture<Map<K, T>> first(final Map<K, ComposableFuture<T>> futures,
                                                         final int numOfSuccess, final long timeout,
                                                         final TimeUnit timeUnit) {
    return Combiner.first(futures, numOfSuccess, false, timeout, timeUnit);
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left,
                                                        final ComposableFuture<T2> right,
                                                        final com.outbrain.ob1k.concurrent.combiners.BiFunction<T1, T2, R> combiner) {
    return Combiner.combine(left, right, combiner);
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left,
                                                        final ComposableFuture<T2> right,
                                                        final FutureBiFunction<T1, T2, R> combiner) {
    return Combiner.combine(left, right, combiner);
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final TriFunction<T1, T2, T3, R> combiner) {
    return Combiner.combine(first, second, third, combiner);
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final FutureTriFunction<T1, T2, T3, R> combiner) {
    return Combiner.combine(first, second, third, combiner);
  }

  public static <T> ComposableFuture<T> any(final ComposableFuture<T> f1, final ComposableFuture<T> f2) {
    return any(Arrays.asList(f1, f2));
  }

  public static <T> ComposableFuture<T> any(final ComposableFuture<T> f1, final ComposableFuture<T> f2,
                                            final ComposableFuture<T> f3) {
    return any(Arrays.asList(f1, f2, f3));
  }

  public static <T> ComposableFuture<T> any(final List<ComposableFuture<T>> futures) {
    return Combiner.any(futures);
  }

  public static <T, R> ComposableFuture<R> foreach(final List<T> elements, final R zero,
                                                   final ForeachHandler<T, R> handler) {
    ComposableFuture<R> result = fromValue(zero);
    for (final T element : elements) {
      result = result.flatMap(elementResult -> handler.handle(element, elementResult));
    }
    return result;
  }

  public static <R> ComposableFuture<R> repeat(final int iterations, final R zero,
                                               final FutureSuccessHandler<R, R> handler) {
    ComposableFuture<R> result = fromValue(zero);
    for (int i = 0; i < iterations; ++i) {
      result = result.flatMap(handler::handle);
    }
    return result;
  }

  /**
   * Creates a new future with one level of nesting flattened, this method is equivalent to flatMap(identity)
   *
   * @param future the future to flatten
   * @param <R>    the future value type.
   * @return a new future that will produce the result from invocation of 2 levels of futures.
   */
  public static <R> ComposableFuture<R> flatten(ComposableFuture<ComposableFuture<R>> future) {
    return future.flatMap(Function.identity());
  }

  /**
   * Execute the producer on each element in the list in batches.
   * every batch is executed in parallel and the next batch begins only after the previous one ended.
   * An error in one of the futures produced by the producer will end the flow and return a future containing the error
   * <p>
   * The batches are created in order i.e. the first batch is the first section of the list and so on.
   * The order within a single batch is undefined since it runs in parallel.
   *
   * @param elements  the input to the producer
   * @param batchSize how many items will be processed in parallel
   * @param producer  produces a future based on input from the element list
   * @param <T>       the type of the elements in the input list
   * @param <R>       the result type of the future returning from the producer
   * @return a future containing a list of all the results produced by the producer.
   */
  public static <T, R> ComposableFuture<List<R>> batch(final List<T> elements, final int batchSize,
                                                       final FutureSuccessHandler<T, R> producer) {
    return batch(elements, 0, batchSize, producer);
  }

  private static <T, R> ComposableFuture<List<R>> batch(final List<T> elements, final int index, final int batchSize,
                                                        final FutureSuccessHandler<T, R> producer) {
    if (index >= elements.size()) {
      return ComposableFutures.fromValue(Collections.<R>emptyList());
    }

    final List<ComposableFuture<R>> singleBatch = new ArrayList<>(batchSize);
    for (int i = index; i < index + batchSize && i < elements.size(); i++) {
      singleBatch.add(producer.handle(elements.get(i)));
    }

    final ComposableFuture<List<R>> batchRes = all(true, singleBatch);
    return batchRes.flatMap(batchResult -> {
      final ComposableFuture<List<R>> rest = batch(elements, index + batchSize, batchSize, producer);
      return rest.map(result -> {
        final List<R> res = new ArrayList<>(result.size() + batchResult.size());
        res.addAll(batchResult);
        res.addAll(result);
        return res;
      });
    });
  }

  /**
   * Execute the producer on each element in the list in parallel.
   * Each parallel flow will opportunistically try to process
   * The next available item on the list upon completion of the previous one.
   * Use this method when execution time for each element is highly irregular so that slow elements
   * In the beginning of the list won't necessarily hold back the rest of the execution.
   * <p>
   * An error in one of the futures produced by the producer will end the flow and return a future containing the error
   *
   * @param elements    the input to the producer
   * @param parallelism how many items will be processed in parallel
   * @param producer    produces a future based on input from the element list
   * @param <T>         the type of the elements in the input list
   * @param <R>         the result type of the future returning from the producer
   * @return a future containing a list of all the results produced by the producer.
   */
  public static <T, R> ComposableFuture<List<R>> batchUnordered(final List<T> elements, final int parallelism,
                                                                final FutureSuccessHandler<T, R> producer) {
    Preconditions.checkArgument(parallelism > 0, "Parallelism must be > 0, not %s", parallelism);

    final AtomicIntegerArray nodesState = new AtomicIntegerArray(elements.size());

    @SuppressWarnings("unchecked") final R[] results = (R[]) new Object[elements.size()];

    final AtomicInteger pendingNodeLowerBound = new AtomicInteger(parallelism + 1);
    final List<ComposableFuture<Void>> futures = new ArrayList<>(parallelism);
    for (int rootNode = 1; rootNode <= parallelism; rootNode++) {
      futures.add(processTree(elements, rootNode, nodesState, results, pendingNodeLowerBound, producer, true));
    }

    return ComposableFutures.all(true, futures).map(__ -> Arrays.asList(results));
  }

  /**
   * @param elements
   * @param rootNode:              The root of the tree currently being processed.
   * @param nodesState:            Pending node is marked with 0, processed node with 1.
   * @param results:               An array containing the results of the computation.
   * @param pendingNodeLowerBound: All elements with index < pendingNodeLowerBound are
   *                               either processed, or going to be processed by the thread modifying pendingNodeLowerBound.
   * @param producer
   * @param jumpWhenDone:          Indicates whether to continue processing from the pending node
   *                               with smallest index or return the processed results.
   * @param <T>
   * @param <R>
   * @return A ComposableFuture that will eventually apply producer to a subset of elements.
   * The computation progresses serially through a subset of elements in no particular order.
   * Since the algorithm is recursive, effort is made to limit the recursion depth.
   * The list of elements is treated as a binary tree rooted at index 1 (which maps to elements index 0),
   * where each tree rooted at index i has left subtree rooted at 2 * i and right subtree rooted
   * at 2 * i + 1.
   * Given an input rootNode, the subtree rooted at rootNode is traversed Pre-order serially.
   * When the traversal of a complete subtree ends,
   * or a processed element is encountered, the traversal proceeds
   * from the top leftmost pending node (smallest index).
   * This traversal strategy minimizes contention on the same part of the tree
   * by separate flows, and decreases the recursion depth.
   */
  private static <T, R> ComposableFuture<Void> processTree(final List<T> elements,
                                                           final int rootNode,
                                                           final AtomicIntegerArray nodesState,
                                                           final R[] results,
                                                           final AtomicInteger pendingNodeLowerBound,
                                                           final FutureSuccessHandler<T, R> producer,
                                                           final boolean jumpWhenDone) {
    if (rootNode > elements.size()) {
      return ComposableFutures.fromNull();
    }

    if (!nodesState.compareAndSet(rootNode - 1, 0, 1)) {
      // A parallel flow is working on our subtree, let's look for another subtree.
      return jump(elements, nodesState, results, pendingNodeLowerBound, producer);
    }

    final ComposableFuture<R> root = producer.handle(elements.get(rootNode - 1));

    return root.flatMap(rootResult -> {
      results[rootNode - 1] = rootResult;
      final int leftNode = rootNode << 1;
      final ComposableFuture<Void> left = processTree(elements, leftNode, nodesState, results, pendingNodeLowerBound, producer, false);
      return left.flatMap(leftResults -> {
        final int rightNode = leftNode + 1;
        final ComposableFuture<Void> right = processTree(elements, rightNode, nodesState, results, pendingNodeLowerBound, producer, false);
        return jumpWhenDone ? right.flatMap(rightResults -> jump(elements, nodesState, results, pendingNodeLowerBound, producer)) : right;
      });
    });
  }

  private static <T, R> ComposableFuture<Void> jump(final List<T> elements,
                                                    final AtomicIntegerArray nodesState,
                                                    final R[] results,
                                                    final AtomicInteger pendingNodeLowerBound,
                                                    final FutureSuccessHandler<T, R> producer) {
    int node = pendingNodeLowerBound.get();

    // Find the top leftmost pending element
    for (; node <= elements.size() && nodesState.get(node - 1) == 1; node++) ;

    // We may set pendingNodeLowerBound to a lower value than its latest value,
    // but it won't break the invariant that for all x < pendingNodeLowerBound.get(), nodesState.get(x - 1) == 1
    pendingNodeLowerBound.set(node + 1);
    return processTree(elements, node, nodesState, results, pendingNodeLowerBound, producer, true);
  }

  /**
   * Execute the producer on each element in the list in batches and return a stream of batch results.
   * Every batch is executed in parallel and the next batch begins only after the previous one ended.
   * The result of each batch is the next element in the stream.
   * An error in one of the futures produced by the producer will end the stream with the error
   *
   * @param elements  the input to the producer
   * @param batchSize how many items will be processed in parallel
   * @param producer  produces a future based on input from the element list
   * @param <T>       the type of the elements in the input list
   * @param <R>       the result type of the future returning from the producer
   * @return a stream containing the combined result of each batch
   */
  public static <T, R> Observable<List<R>> batchToStream(final List<T> elements, final int batchSize,
                                                         final FutureSuccessHandler<T, R> producer) {
    return Observable.create(subscriber -> batchToStream(elements, batchSize, 0, subscriber, producer));
  }

  private static <T, R> void batchToStream(final List<T> elements, final int batchSize, final int index,
                                           final Subscriber<? super List<R>> subscriber,
                                           final FutureSuccessHandler<T, R> producer) {

    if (index >= elements.size()) {
      subscriber.onCompleted();
    } else {
      final List<ComposableFuture<R>> singleBatch = new ArrayList<>(batchSize);
      for (int i = index; i < index + batchSize && i < elements.size(); i++) {
        singleBatch.add(producer.handle(elements.get(i)));
      }

      final ComposableFuture<List<R>> batchRes = all(true, singleBatch);
      batchRes.consume(result -> {
        if (result.isSuccess()) {
          subscriber.onNext(result.getValue());
          batchToStream(elements, batchSize, index + batchSize, subscriber, producer);
        } else {
          subscriber.onError(result.getError());
        }
      });
    }
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
    return submitRes.flatMap(identity());
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

  public static <T, S> ComposableFuture<S> from(final T value,
                                                final com.google.common.base.Function<? super T, ? extends S> function) {
    return submit(() -> function.apply(value));
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

  public static <T> ComposableFuture<T> scheduleFuture(final Callable<ComposableFuture<T>> task, final long delay,
                                                       final TimeUnit unit) {
    final ComposableFuture<ComposableFuture<T>> schedule = schedule(task, delay, unit);
    return schedule.flatMap(identity());
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
  public static <T> ComposableFuture<T> withTimeout(final ComposableFuture<T> future, final long duration,
                                                    final TimeUnit unit) {
    return future.withTimeout(SchedulerServiceHolder.INSTANCE, duration, unit);
  }

  /**
   * retries a given operation N times for each computed Failed future.
   *
   * @param retries max amount of retries
   * @param action  the eager future provider
   * @param <T>     the future type
   * @return the composed result.
   */
  public static <T> ComposableFuture<T> retry(final int retries, final FutureAction<T> action) {
    return retry(retries, __ -> action.execute());
  }

  /**
   * retries a given operation N times for each computed Failed future.
   *
   * @param retries  attempts amount
   * @param producer future producer
   * @param <T>      result value type
   * @return future containing retry result value
   */
  @SuppressWarnings("unchecked")
  public static <T> ComposableFuture<T> retry(final int retries, final Function<Integer, ComposableFuture<T>> producer) {
    final BiFunction<BiFunction, Integer, ComposableFuture<T>> retriable = (f, attempt) ->
      Try.apply(() -> producer.apply(attempt)).
        recover(ComposableFutures::fromError).
        map(action -> action.recoverWith(error -> {
            if (attempt >= retries) {
              return fromError(error);
            }

            // we can't safely use generics here since we'll get into infinite nested generics
            return (ComposableFuture<T>) f.apply(f, attempt + 1);
          })
        ).getValue();

    return retriable.apply(retriable, 0);
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
   * @deprecated please define execution cap outside of retry context
   */
  @Deprecated
  public static <T> ComposableFuture<T> retry(final int retries, final long duration, final TimeUnit unit,
                                              final FutureAction<T> action) {
    return action.execute().withTimeout(duration, unit).recoverWith(error -> {
      if (retries < 1) {
        return ComposableFutures.fromError(error);
      }

      return retry(retries - 1, action);
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
  public static <T> ComposableFuture<T> doubleDispatch(final long duration, final TimeUnit unit,
                                                       final FutureAction<T> action) {
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
  public static <T> ComposableFuture<T> doubleDispatch(final ComposableFuture<T> future, final long duration,
                                                       final TimeUnit unit) {
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
    return Observable.create(subscriber -> {
      final AtomicInteger counter = new AtomicInteger(futures.size());
      final AtomicBoolean errorTrigger = new AtomicBoolean(false);

      for (final ComposableFuture<T> future : futures) {
        future.consume(provideObserverResult(subscriber, counter, errorTrigger, failOnError));
      }
    });
  }

  /**
   * creates new cold observable, given future provider,
   * on each subscribe will consume the provided future
   * and repeat until stop criteria will exists
   * each result will be emitted to the stream
   *
   * @param futureProvider the future provider
   * @param <T>            the stream type
   * @return the stream
   */
  public static <T> Observable<T> toColdObservable(final RecursiveFutureProvider<T> futureProvider) {
    return Observable.create(new Observable.OnSubscribe<T>() {
      @Override
      public void call(final Subscriber<? super T> subscriber) {
        recursiveChain(subscriber, futureProvider.createStopCriteria());
      }

      private void recursiveChain(final Subscriber<? super T> subscriber, final Predicate<T> stopCriteria) {
        futureProvider.provide().consume(result -> {
          if (result.isSuccess()) {
            final T value = result.getValue();
            subscriber.onNext(value);
            if (stopCriteria.apply(value)) {
              subscriber.onCompleted();
            } else {
              recursiveChain(subscriber, stopCriteria);
            }
          } else {
            subscriber.onError(result.getError());
          }
        });
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
      future.consume(provideObserverResult(subject, counter, errorTrigger, failOnError));
    }

    return subject;
  }

  /**
   * creates new observable given future provider,
   * translating the future results into stream.
   * the sequence will be evaluated on subscribe.
   *
   * @param provider the future provider for translation
   * @param <T>      the stream type
   * @return the stream
   */
  public static <T> Observable<T> toObservable(final FutureProvider<T> provider) {
    return Observable.create(new FutureProviderToStreamHandler<>(provider));
  }

  private static <T> Consumer<T> provideObserverResult(final rx.Observer<? super T> observer,
                                                       final AtomicInteger counter,
                                                       final AtomicBoolean errorTrigger,
                                                       final boolean failOnError) {
    return result -> {
      if (result.isSuccess()) {
        observer.onNext(result.getValue());
        if (counter.decrementAndGet() == 0) {
          observer.onCompleted();
        }
      } else {
        if (failOnError) {
          if (errorTrigger.compareAndSet(false, true)) {
            observer.onError(result.getError());
          }
          counter.set(0);
        } else {
          if (counter.decrementAndGet() == 0) {
            observer.onCompleted();
          }
        }
      }
    };
  }

  public static ExecutorService getExecutor() {
    return ExecutorServiceHolder.INSTANCE;
  }

  public static Scheduler getScheduler() {
    return SchedulerServiceHolder.INSTANCE;
  }

  private static class ExecutorServiceHolder {
    private static final ExecutorService INSTANCE =
      createExecutor(Configuration.getExecutorCoreSize(), Configuration.getExecutorMaxSize());

    private static ExecutorService createExecutor(final int coreSize, final int maxSize) {
      final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(coreSize, maxSize,
        0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
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
}
