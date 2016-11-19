package com.outbrain.ob1k.concurrent.examples;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.combiners.BiFunction;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Covers most of ComposableFutures utilities
 *
 * @see ComposableFutures
 * @author marenzon
 */
public class ComposableFuturesHelpers {

  public static void main(final String[] args) {
    // Basics
    fromValues();
    promises();

    // Combiners
    combineFutures();
    synchronizeFutures();
    anyFuture();
    firstN();

    // Utilities
    batchIo();
    doubleDispatch();
    repeatSome();
  }

  public static void fromValues() {
    // A future which contains immediate value that we have
    final ComposableFuture<String> immediateFuture = ComposableFutures.fromValue("immediate value");

    // A future which contains immediate error that we have. Note that the generic not related to the exception
    final ComposableFuture<String> immediateError = ComposableFutures.fromError(new IOException("sucks"));

    // A future being created from ob1k's Try object.
    final ComposableFuture<String> futureFromTry = ComposableFutures.fromTry(Try.fromValue("try"));

    // .submit() gives you the ability sending tasks to a thread-pool (not always), and receiving a future back.
    // note the boolean being passed, .submit() contains few overloads, one accepts thread-pool, one accepts task only
    // and runs it on the same thread, and one accepts boolean telling if to run the task on ob1k's thread-pool or not.
    // if you need to do sync IO, always pass true.
    final ComposableFuture<String> futureFromSync = ComposableFutures.submit(true, () -> "sync io");

    // .schedule() schedules a task on specified time. The task will occur only once,
    // same as ScheduledExecutorService#schedule()
    final ComposableFuture<String> scheduledTask = ComposableFutures.schedule(() ->
      "do some operation in the future", 5, SECONDS);
  }

  public static void promises() {
    // Promise is a container which holds future at its back, but provides you the ability
    // of setting the value by yourself. For example, if you need to transform some callback method to ComposableFuture,
    // you can do it with ease with promise.
    final ComposablePromise<String> promise = ComposableFutures.newPromise();

    // The future of the promise
    final ComposableFuture<String> future = promise.future();
    future.consume(result -> System.out.println("Result is: " + result.getValue()));
    Executors.newScheduledThreadPool(1).schedule(() -> promise.set("hello world"), 100, MILLISECONDS);
  }

  public static void synchronizeFutures() {
    final ComposableFuture<String> asyncOp1 = someDelayedIo("hello world 1", 10);
    final ComposableFuture<String> asyncOp2 = someDelayedIo("hello world 2", 20);
    final ComposableFuture<String> asyncOp3 = someIo("hello world 3");

    // all(futures..) returns ComposableFuture<List<T>>. All futures from same generic type.
    ComposableFutures.all(asyncOp1, asyncOp2, asyncOp3).
      consume(result -> System.out.println("All three futures results: " + result.getValue()));

    final ComposableFuture<String> asyncOp5 = someDelayedIo("hello world 1", 10);
    final ComposableFuture<String> asyncOp6 = errorIo(new IOException("ergh.. network.. bad.."));

    // same as above, but will fail as fast as some future fails
    ComposableFutures.all(true, asList(asyncOp5, asyncOp6)).
      consume(result -> System.out.println("Will be the first error consumed: " + result.getError().getMessage()));
  }

  public static void anyFuture() {
    final ComposableFuture<String> asyncOp1 = someDelayedIo("too slow", 5);
    final ComposableFuture<String> asyncOp2 = someDelayedIo("very slow", 10);
    final ComposableFuture<String> asyncOp3 = someIo("will be returned");

    // any(futures..) returns ComposableFuture<T>, which will be the first future that returned a value.
    ComposableFutures.any(asyncOp1, asyncOp2, asyncOp3).
      consume(result -> System.out.println("First returned future value: " + result.getValue()));
  }

  public static void firstN() {
    final ComposableFuture<String> asyncOp1 = someDelayedIo("too slow", 5);
    final ComposableFuture<String> asyncOp2 = someDelayedIo("very slow", 10);
    final ComposableFuture<String> asyncOp3 = someIo("will be returned");

    final HashMap<String, ComposableFuture<String>> futures = Maps.newHashMap();
    futures.put("first", asyncOp1);
    futures.put("second", asyncOp2);
    futures.put("third", asyncOp3);

    // .first(futures.., N) returns first N successful futures that have been completed
    final ComposableFuture<Map<String, String>> firstValuesReturned = ComposableFutures.first(futures, 2);
    firstValuesReturned.consume(result -> System.out.println(result.getValue()));
  }

  public static void combineFutures() {
    final ComposableFuture<String> asyncOp1 = someIo("hello world");
    final ComposableFuture<Integer> asyncOp2 = someIo(1);

    // .combine(futures.., combiner function), returns a new ComposableFuture<V>, by provided combiner for
    // provided future values
    ComposableFutures.combine(asyncOp1, asyncOp2, (BiFunction<String, Integer, String>) (left, right) ->
      left + " " + right).
    consume(result -> System.out.println("Combined result: " + result.getValue()));
  }

  public static void batchIo() {
    final List<String> strings = Collections.nCopies(50, "hello world");

    // .batch(list<elements>, batch size, handler) -> creates N concurrent operations at a time,
    // providing you the ability of doing batch computation over collection of values.
    ComposableFutures.batch(strings, 10, value -> someDelayedIo(value, 5)).
      consume(result -> System.out.println("Batched list: " + result.getValue()));
  }

  public static void doubleDispatch() {
    // .doubleDispatch does another dispatch after defined period of time if future hasn't returned a result yet.
    // it comes in two flavours - one is providing a future task that will return the future to double dispatch on,
    // and second one is providing the future itself and the double dispatch will be on the same action.
    // NOTE: the second one is possible only for LAZY FUTURE, thus you probably will use only the first one,
    // unless you understand the context you're running in.
    ComposableFutures.doubleDispatch(250, MILLISECONDS, () -> someDelayedIo("will be also called", 200)).
      consume(result -> System.out.println("First returned value is: " + result.getValue()));

    ComposableFutures.doubleDispatch(ComposableFutures.fromValueLazy("lazy future"), 20, MILLISECONDS);
  }

  public static void repeatSome() {
    // .repeat(times, initial value, op) will repeat your async operation on top the initial value N times
    ComposableFutures.repeat(5, "initial value", ComposableFuturesHelpers::someIo);

    // .retry(times, [optional timeout], action) will retry your async operation N times, and will
    // stop on first successful returned value
    ComposableFutures.retry(5, ComposableFuturesHelpers::randomIo);
  }

  public static ComposableFuture<String> randomIo() {
    if (ThreadLocalRandom.current().nextInt(100) % 10 == 0) {
      return errorIo(new IOException("BADD!!!!"));
    }
    return someIo("GOOD!!!!");
  }

  public static <T> ComposableFuture<T> someIo(final T initialValue) {
    return ComposableFutures.fromValue(initialValue);
  }

  public static <T> ComposableFuture<T> someDelayedIo(final T initialValue, final int timeMs) {
    return ComposableFutures.schedule(() -> initialValue, timeMs, MILLISECONDS);
  }

  public static <T> ComposableFuture<T> errorIo(final Exception exception) {
    return ComposableFutures.fromError(exception);
  }
}