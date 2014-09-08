package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.AggregatedException;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.OnResultHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by aronen on 9/2/14.
 *
 * combines two or more futures into one.
 */
public class Combiner {
  private static final Logger logger = LoggerFactory.getLogger(Combiner.class);

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final List<ComposableFuture<T>> futures) {
    final int size = futures.size();
    if (size == 0) {
      final List<T> empty = new ArrayList<>();
      return ComposableFutures.fromValue(empty);
    }

    final AtomicInteger counter = new AtomicInteger();
    final ComposablePromise<List<T>> res = ComposableFutures.newPromise();
    final Queue<T> results = new ConcurrentLinkedQueue<>();
    final Queue<Throwable> errors = new ConcurrentLinkedQueue<>();

    for (final ComposableFuture<T> future : futures) {
      future.onResult(new OnResultHandler<T>() {
        @Override
        public void handle(final ComposableFuture<T> result) {
          try {
            final T singleRes = result.get();
            results.add(singleRes);
          } catch (final ExecutionException e) {
            errors.add(e.getCause() != null ? e.getCause() : e);
          } catch (final Exception e) {
            errors.add(e);
          }

          if (counter.incrementAndGet() == size) {
            if (errors.isEmpty() || !failOnError) {
              if (logger.isDebugEnabled()) {
                for (final Throwable e : errors) {
                  logger.debug(e.getMessage());
                }
              }
              res.set(new ArrayList<>(results));
            } else {
              res.setException(new AggregatedException(new ArrayList<>(errors)));
            }
          }
        }
      });
    }

    return res;
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError, final Map<K, ComposableFuture<T>> futures) {
    final int size = futures.size();
    if (size == 0) {
      final Map<K, T> empty = new HashMap<>();
      return ComposableFutures.fromValue(empty);
    }

    final ComposablePromise<Map<K, T>> res = ComposableFutures.newPromise();
    final Map<K, T> resultMap = new ConcurrentHashMap<>();
    final AtomicInteger counter = new AtomicInteger();
    final Queue<Throwable> errors = new ConcurrentLinkedQueue<>();

    for (final K key : futures.keySet()) {
      final ComposableFuture<T> future = futures.get(key);
      future.onResult(new OnResultHandler<T>() {
        @Override
        public void handle(final ComposableFuture<T> result) {
          try {
            final T value = result.get();
            if (value != null) {
              resultMap.put(key, value);
            }
          } catch (final ExecutionException e) {
            errors.add(e.getCause() != null ? e.getCause() : e);
          } catch (final Exception e) {
            errors.add(e);
          }

          if (counter.incrementAndGet() == size) {
            if (errors.isEmpty() || (!failOnError && errors.size() < size)) {
              if (logger.isDebugEnabled()) {
                for (final Throwable e : errors) {
                  logger.debug(e.getMessage());
                }
              }
              res.set(resultMap);
            } else {
              res.setException(new AggregatedException(new ArrayList<>(errors)));
            }
          }

        }
      });
    }

    return res;
  }


  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right, final BiFunction<T1, T2, R> combiner) {
    final ComposableFuture<BiContainer<T1, T2>> upliftLeft = left.continueOnSuccess(new SuccessHandler<T1, BiContainer<T1, T2>>() {
      @Override
      public BiContainer<T1, T2> handle(final T1 result) throws ExecutionException {
        return new BiContainer<>(result, null);
      }
    });

    final ComposableFuture<BiContainer<T1, T2>> upliftRight = right.continueOnSuccess(new SuccessHandler<T2, BiContainer<T1, T2>>() {
      @Override
      public BiContainer<T1, T2> handle(final T2 result) throws ExecutionException {
        return new BiContainer<>(null, result);
      }
    });

    final HashMap<String, ComposableFuture<BiContainer<T1, T2>>> elements = new HashMap<>();
    final String leftKey = "left";
    final String rightKey = "right";
    elements.put(leftKey, upliftLeft);
    elements.put(rightKey, upliftRight);

    return all(true, elements).continueOnSuccess(new SuccessHandler<Map<String, BiContainer<T1, T2>>, R>() {
      @Override
      public R handle(final Map<String, BiContainer<T1, T2>> result) throws ExecutionException {
        final BiContainer<T1, T2> leftContainer = result.get(leftKey);
        final BiContainer<T1, T2> rightContainer = result.get(rightKey);
        return combiner.apply(leftContainer.left, rightContainer.right);
      }
    });
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final TriFunction<T1, T2, T3, R> combiner) {

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftFirst = first.continueOnSuccess(new SuccessHandler<T1, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T1 result) throws ExecutionException {
        return new TriContainer<>(result, null, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftSecond = second.continueOnSuccess(new SuccessHandler<T2, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T2 result) throws ExecutionException {
        return new TriContainer<>(null, result, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftThird = third.continueOnSuccess(new SuccessHandler<T3, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T3 result) throws ExecutionException {
        return new TriContainer<>(null, null, result);
      }
    });

    final HashMap<String, ComposableFuture<TriContainer<T1, T2, T3>>> elements = new HashMap<>();
    final String firstKey = "first";
    final String secondKey = "second";
    final String thirdKey = "third";

    elements.put(firstKey, upliftFirst);
    elements.put(secondKey, upliftSecond);
    elements.put(thirdKey, upliftThird);

    return all(true, elements).continueOnSuccess(new SuccessHandler<Map<String, TriContainer<T1, T2, T3>>, R>() {
      @Override
      public R handle(final Map<String, TriContainer<T1, T2, T3>> result) throws ExecutionException {
        final TriContainer<T1, T2, T3> firstContainer = result.get(firstKey);
        final TriContainer<T1, T2, T3> secondContainer = result.get(secondKey);
        final TriContainer<T1, T2, T3> thirdContainer = result.get(thirdKey);

        return combiner.apply(firstContainer.first, secondContainer.second, thirdContainer.third);
      }
    });

  }
}
