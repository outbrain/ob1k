package com.outbrain.ob1k.concurrent.combiners;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.UncheckedExecutionException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;

/**
 * combines two or more futures into one.
 *
 * @author aronen on 9/2/14.
 */
public class Combiner {

  public static <T> ComposableFuture<T> any(final Iterable<ComposableFuture<T>> elements) {
    return ComposableFutures.build(consumer -> {
      final AtomicBoolean done = new AtomicBoolean();
      for (final ComposableFuture<T> future : elements) {
        future.consume(result -> {
          if (done.compareAndSet(false, true)) {
            consumer.consume(result);
          }
        });
      }
    });
  }

  public static <T> ComposableFuture<List<T>> all(final boolean failOnError, final Iterable<ComposableFuture<T>> elements) {
    final Map<Integer, ComposableFuture<T>> elementsMap = new HashMap<>();
    int index = 0;
    for (final ComposableFuture<T> element : elements) {
      elementsMap.put(index++, element);
    }

    return all(failOnError, elementsMap).map(result -> new ArrayList<>(result.values()));
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

    if (elements.isEmpty()) {
      final Map<K, T> empty = new HashMap<>();
      return fromValue(empty);
    }

    return ComposableFutures.build(consumer -> {
      final AtomicReferenceArray<KeyValue<K, T>> results = new AtomicReferenceArray<>(elements.size());
      final AtomicReference<Status> status = new AtomicReference<>(new Status(elements.size(), numOfSuccess, 0, 0, false));
      int counter = 0;

      if (timeout != null) {
        schedule(() -> {
          while (true) {
            final Status currentStatus = status.get();
            if (currentStatus.isDone())
              break;

            final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                currentStatus.results, currentStatus.successfulResults, true);

            final boolean success = status.compareAndSet(currentStatus, newStatus);
            if (success) {
              consumer.consume(Try.fromValue(collectResults(results)));
              break;
            }
          }
          return null;
        }, timeout, timeUnit);
      }

      for (final Map.Entry<K, ComposableFuture<T>> element : elements.entrySet()) {
        final ComposableFuture<T> future = element.getValue();
        final K key = element.getKey();
        final int index = counter;
        counter++;

        future.consume(result -> {
          if (result.isSuccess()) {
            final T element1 = result.getValue();
            results.set(index, new KeyValue<>(key, element1));

            while (true) {
              final Status currentStatus = status.get();
              if (currentStatus.isDone())
                break;

              final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                  currentStatus.results + 1, currentStatus.successfulResults + 1, false);

              final boolean success = status.compareAndSet(currentStatus, newStatus);
              if (success) {
                if (newStatus.isDone()) {
                  consumer.consume(Try.fromValue(collectResults(results)));
                }
                break;
              }
            }

          } else {
            final Throwable error = result.getError();
            while (true) {
              final Status currentStatus = status.get();
              if (currentStatus.isDone())
                break;

              final Status newStatus = new Status(currentStatus.total, currentStatus.minSuccessful,
                  currentStatus.results + 1, currentStatus.successfulResults, failOnError);

              final boolean success = status.compareAndSet(currentStatus, newStatus);
              if (success) {
                if (failOnError) {
                  consumer.consume(Try.fromError(error));
                } else {
                  if (newStatus.isDone()) {
                    consumer.consume(Try.fromValue(collectResults(results)));
                  }
                }
                break;
              }
            }
          }
        });
      }
    });
  }

  private static <K, T> Map<K, T> collectResults(final AtomicReferenceArray<KeyValue<K, T>> elements) {
    final Map<K, T> result = new HashMap<>();
    for (int i=0; i< elements.length(); i++) {
      final KeyValue<K, T> element = elements.get(i);
      if (element != null && element.value != null) {
        result.put(element.key, element.value);
      }
    }

    return result;
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right, final BiFunction<T1, T2, R> combiner) {
    final ComposableFuture<BiContainer<T1, T2>> upliftLeft = left.map(result -> new BiContainer<>(result, null));

    final ComposableFuture<BiContainer<T1, T2>> upliftRight = right.map(result -> new BiContainer<>(null, result));

    final HashMap<String, ComposableFuture<BiContainer<T1, T2>>> elements = new HashMap<>();
    final String leftKey = "left";
    final String rightKey = "right";
    elements.put(leftKey, upliftLeft);
    elements.put(rightKey, upliftRight);

    return all(true, elements).map(result -> {
      final BiContainer<T1, T2> leftContainer = result.get(leftKey);
      final BiContainer<T1, T2> rightContainer = result.get(rightKey);
      try {
        return combiner.apply(leftContainer.left, rightContainer.right);
      } catch (final ExecutionException e) {
        throw new UncheckedExecutionException(e.getCause());
      }
    });
  }

  public static <T1, T2, R> ComposableFuture<R> combine(final ComposableFuture<T1> left, final ComposableFuture<T2> right, final FutureBiFunction<T1, T2, R> combiner) {
    final ComposableFuture<BiContainer<T1, T2>> upliftLeft = left.map(result -> new BiContainer<>(result, null));

    final ComposableFuture<BiContainer<T1, T2>> upliftRight = right.map(result -> new BiContainer<>(null, result));

    final HashMap<String, ComposableFuture<BiContainer<T1, T2>>> elements = new HashMap<>();
    final String leftKey = "left";
    final String rightKey = "right";
    elements.put(leftKey, upliftLeft);
    elements.put(rightKey, upliftRight);

    return all(true, elements).flatMap(result -> {
      final BiContainer<T1, T2> leftContainer = result.get(leftKey);
      final BiContainer<T1, T2> rightContainer = result.get(rightKey);
      return combiner.apply(leftContainer.left, rightContainer.right);
    });
  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final TriFunction<T1, T2, T3, R> combiner) {

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftFirst = first.map(result -> new TriContainer<>(result, null, null));

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftSecond = second.map(result -> new TriContainer<>(null, result, null));

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftThird = third.map(result -> new TriContainer<>(null, null, result));

    final HashMap<String, ComposableFuture<TriContainer<T1, T2, T3>>> elements = new HashMap<>();
    final String firstKey = "first";
    final String secondKey = "second";
    final String thirdKey = "third";

    elements.put(firstKey, upliftFirst);
    elements.put(secondKey, upliftSecond);
    elements.put(thirdKey, upliftThird);

    return all(true, elements).map(result -> {
      final TriContainer<T1, T2, T3> firstContainer = result.get(firstKey);
      final TriContainer<T1, T2, T3> secondContainer = result.get(secondKey);
      final TriContainer<T1, T2, T3> thirdContainer = result.get(thirdKey);

      try {
        return combiner.apply(firstContainer.first, secondContainer.second, thirdContainer.third);
      } catch (final ExecutionException e) {
        throw new UncheckedExecutionException(e.getCause());
      }
    });

  }

  public static <T1, T2, T3, R> ComposableFuture<R> combine(final ComposableFuture<T1> first,
                                                            final ComposableFuture<T2> second,
                                                            final ComposableFuture<T3> third,
                                                            final FutureTriFunction<T1, T2, T3, R> combiner) {

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftFirst = first.map(result -> new TriContainer<>(result, null, null));

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftSecond = second.map(result -> new TriContainer<>(null, result, null));

    final ComposableFuture<TriContainer<T1, T2, T3>> upliftThird = third.map(result -> new TriContainer<>(null, null, result));

    final HashMap<String, ComposableFuture<TriContainer<T1, T2, T3>>> elements = new HashMap<>();
    final String firstKey = "first";
    final String secondKey = "second";
    final String thirdKey = "third";

    elements.put(firstKey, upliftFirst);
    elements.put(secondKey, upliftSecond);
    elements.put(thirdKey, upliftThird);

    return all(true, elements).flatMap(result -> {
      final TriContainer<T1, T2, T3> firstContainer = result.get(firstKey);
      final TriContainer<T1, T2, T3> secondContainer = result.get(secondKey);
      final TriContainer<T1, T2, T3> thirdContainer = result.get(thirdKey);

      return combiner.apply(firstContainer.first, secondContainer.second, thirdContainer.third);
    });

  }

}
