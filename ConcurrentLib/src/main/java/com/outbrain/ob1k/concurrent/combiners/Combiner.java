package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.OnErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.OnSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by aronen on 9/2/14.
 *
 * combines two or more futures into one.
 */
public class Combiner {
  private static final Logger logger = LoggerFactory.getLogger(Combiner.class);

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final Iterable<ComposableFuture<T>> elements) {
    Map<Integer, ComposableFuture<T>> elementsMap = new HashMap<>();
    int index = 0;
    for(ComposableFuture<T> element : elements) {
      elementsMap.put(index++, element);
    }

    return all(failOnError, elementsMap).continueOnSuccess(new SuccessHandler<Map<Integer, T>, List<T>>() {
      @Override
      public List<T> handle(Map<Integer, T> result) throws ExecutionException {
        return new ArrayList<>(result.values());
      }
    });
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError, final Map<K, ComposableFuture<T>> elements) {
    final ComposablePromise<Map<K, T>> promise = ComposableFutures.newPromise();
    final Map<K, T> result = new ConcurrentSkipListMap<>();
    if (elements.isEmpty()) {
      return ComposableFutures.fromValue(result);
    }

    final AtomicInteger counter = new AtomicInteger(elements.size());
    for (Map.Entry<K, ComposableFuture<T>> element : elements.entrySet()) {
      ComposableFuture<T> future = element.getValue();
      final K key = element.getKey();

      future.onSuccess(new OnSuccessHandler<T>() {
        @Override
        public void handle(T element) {
          if (element != null) {
            result.put(key, element);
          }

          final int count = counter.decrementAndGet();
          if (count == 0) {
            promise.set(result);
          }
        }
      });

      future.onError(new OnErrorHandler() {
        @Override
        public void handle(Throwable error) {
          if (failOnError) {
            promise.setException(error);
          } else {
            final int count = counter.decrementAndGet();
            if (count == 0) {
              promise.set(result);
            }
          }
        }
      });
    }

    return promise;
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

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right, final FutureBiFunction<T1, T2, R> combiner) {
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

    return all(true, elements).continueOnSuccess(new FutureSuccessHandler<Map<String, BiContainer<T1, T2>>, R>() {
      @Override
      public ComposableFuture<R> handle(final Map<String, BiContainer<T1, T2>> result) {
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

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final FutureTriFunction<T1, T2, T3, R> combiner) {

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

    return all(true, elements).continueOnSuccess(new FutureSuccessHandler<Map<String, TriContainer<T1, T2, T3>>, R>() {
      @Override
      public ComposableFuture<R> handle(final Map<String, TriContainer<T1, T2, T3>> result) {
        final TriContainer<T1, T2, T3> firstContainer = result.get(firstKey);
        final TriContainer<T1, T2, T3> secondContainer = result.get(secondKey);
        final TriContainer<T1, T2, T3> thirdContainer = result.get(thirdKey);

        return combiner.apply(firstContainer.first, secondContainer.second, thirdContainer.third);
      }
    });

  }

}
