package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.OnErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.OnSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;

/**
 * combines two or more futures into one.
 *
 * @author aronen on 9/2/14.
 */
public class Combiner {

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final Iterable<ComposableFuture<T>> elements) {
    final Map<Integer, ComposableFuture<T>> elementsMap = new HashMap<>();
    int index = 0;
    for (final ComposableFuture<T> element : elements) {
      elementsMap.put(index++, element);
    }

    return all(failOnError, elementsMap).continueOnSuccess(new SuccessHandler<Map<Integer, T>, List<T>>() {
      @Override
      public List<T> handle(final Map<Integer, T> result) {
        return new ArrayList<>(result.values());
      }
    });
  }

  public static <K, T> ComposableFuture<Map<K, T>> all(final boolean failOnError, final Map<K, ComposableFuture<T>> elements) {
    return first(elements, elements.size(), failOnError, null, null);
  }

  private static class Status {
    final int total;
    final int minSuccessful;

    final int results;
    final int successfulResults;
    final boolean finished;

    private Status(final int total, final int minSuccessful,
                   final int results, final int successfulResults,
                   final boolean finished) {
      this.total = total;
      this.minSuccessful = minSuccessful;
      this.results = results;
      this.successfulResults = successfulResults;
      this.finished = finished;
    }

    boolean isDone() {
      return finished || results == total || successfulResults >= minSuccessful;
    }
  }

  private static class KeyValue<K, V> {
    final K key;
    final V value;

    private KeyValue(final K key, final V value) {
      this.key = key;
      this.value = value;
    }
  }

  public static <K, T> ComposableFuture<Map<K, T>> first(final Map<K, ComposableFuture<T>> elements, final int numOfSuccess,
                                                         final boolean failOnError, final Long timeout, final TimeUnit timeUnit) {

    final ComposablePromise<Map<K, T>> promise = ComposableFutures.newPromise();
    if (elements.isEmpty()) {
      final Map<K, T> empty = new HashMap<>();
      return fromValue(empty);
    }

    @SuppressWarnings("unchecked")
    final KeyValue<K, T>[] results = new KeyValue[elements.size()];

    final AtomicReference<Status> status = new AtomicReference<>(new Status(elements.size(), numOfSuccess, 0, 0, false));
    int counter = 0;

    if (timeout != null) {
      schedule(new Callable<Object>() {
        @Override
        public Object call() throws Exception {
          while (true) {
            final Status currentStatus = status.get();
            if (currentStatus.isDone())
              break;

            final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                currentStatus.results, currentStatus.successfulResults, true);

            final boolean success = status.compareAndSet(currentStatus, newStatus);
            if (success) {
              promise.set(collectResults(results));
              break;
            }
          }
          return null;
        }
      }, timeout, timeUnit);
    }

    for (final Map.Entry<K, ComposableFuture<T>> element : elements.entrySet()) {
      final ComposableFuture<T> future = element.getValue();
      final K key = element.getKey();
      final int index = counter;
      counter++;

      future.onSuccess(new OnSuccessHandler<T>() {
        @Override
        public void handle(final T element) {
          results[index] = new KeyValue<>(key, element);

          while (true) {
            final Status currentStatus = status.get();
            if (currentStatus.isDone())
              break;

            final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                currentStatus.results + 1, currentStatus.successfulResults + 1, false);

            final boolean success = status.compareAndSet(currentStatus, newStatus);
            if (success) {
              if (newStatus.isDone()) {
                promise.set(collectResults(results));
              }
              break;
            }
          }
        }
      });

      future.onError(new OnErrorHandler() {
        @Override
        public void handle(final Throwable error) {
          while (true) {
            final Status currentStatus = status.get();
            if (currentStatus.isDone())
              break;

            final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                    currentStatus.results + 1, currentStatus.successfulResults, failOnError);

            final boolean success = status.compareAndSet(currentStatus, newStatus);
            if (success) {
              if (failOnError) {
                promise.setException(error);
              } else {
                if (newStatus.isDone()) {
                  promise.set(collectResults(results));
                }
              }
              break;
            }
          }
        }
      });
    }

    return promise;
  }

  private static <K, T> Map<K, T> collectResults(final KeyValue<K, T>[] elements) {
    final Map<K, T> result = new HashMap<>();
    for (final KeyValue<K, T> element : elements) {
      if (element != null) {
        result.put(element.key, element.value);
      }
    }

    return result;
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right, final BiFunction<T1, T2, R> combiner) {
    final ComposableFuture<BiContainer<T1, T2>> upliftLeft = left.continueOnSuccess(new SuccessHandler<T1, BiContainer<T1, T2>>() {
      @Override
      public BiContainer<T1, T2> handle(final T1 result) {
        return new BiContainer<>(result, null);
      }
    });

    final ComposableFuture<BiContainer<T1, T2>> upliftRight = right.continueOnSuccess(new SuccessHandler<T2, BiContainer<T1, T2>>() {
      @Override
      public BiContainer<T1, T2> handle(final T2 result) {
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
      public BiContainer<T1, T2> handle(final T1 result) {
        return new BiContainer<>(result, null);
      }
    });

    final ComposableFuture<BiContainer<T1, T2>> upliftRight = right.continueOnSuccess(new SuccessHandler<T2, BiContainer<T1, T2>>() {
      @Override
      public BiContainer<T1, T2> handle(final T2 result) {
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
      public TriContainer<T1, T2, T3> handle(final T1 result) {
        return new TriContainer<>(result, null, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftSecond = second.continueOnSuccess(new SuccessHandler<T2, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T2 result) {
        return new TriContainer<>(null, result, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftThird = third.continueOnSuccess(new SuccessHandler<T3, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T3 result) {
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
      public TriContainer<T1, T2, T3> handle(final T1 result) {
        return new TriContainer<>(result, null, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftSecond = second.continueOnSuccess(new SuccessHandler<T2, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T2 result) {
        return new TriContainer<>(null, result, null);
      }
    });

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftThird = third.continueOnSuccess(new SuccessHandler<T3, TriContainer<T1, T2, T3>>() {
      @Override
      public TriContainer<T1, T2, T3> handle(final T3 result) {
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
