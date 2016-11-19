package com.outbrain.ob1k.concurrent.examples;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This class in meant to show some chaining examples.
 *
 * @author marenzon
 * @see ComposableFuture
 * @see Try
 */
public class BasicComposableFutureUsages {

  public static void main(final String[] args) throws Exception {
    // Standard flow - doing some IO, transforming the value, and consuming the end result.
    standardFlow();

    // Timeouts
    withSomeTimeout();

    // Delay
    delayFuture();

    // Sync mode
    waitForFuture();

    // Wrong usage
    wrongUsage();
  }

  private static void standardFlow() {
    someIo("hello world").
      map(result -> result + ". Maybe I should take a day off?").
      flatMap(result -> errorIo(new IOException("boss not allows :("))).
      recover(error -> "Damn you boss, I'll take one anyway").
      consume(eventualValue -> {
        // eventualValue is a Try<V>, which may hold either a valid result, or error we've not
        // recovered from.
        eventualValue.map(__ -> "┏(-_-)┛┗(-_-\uFEFF )┓┗(-_-)┛┏(-_-)┓").
          recover(__ -> "Argghhhh I WANT A DAY OFF").
          forEach(System.out::println);
      });
  }

  private static void withSomeTimeout() {
    someDelayedIo("hello world", 2000).
      map(result -> "hello you too!").
      withTimeout(1000, MILLISECONDS). // We don't want to wait more than 1s for chain computation
      consume(result -> System.out.println("The operation is: " + result.toString()));
  }

  private static void waitForFuture() throws ExecutionException, InterruptedException {
    // with .get([optional] timeout), we can stuck the current thread to wait for the result.
    // this CAN NOT be done in async context.
    someIo("hello world").get();
  }

  private static void delayFuture() throws ExecutionException, InterruptedException {
    // #delay method delays computation value by provided duration, useful
    // if needed to create chain of operations with delays between them
    someIo("hello world").delay(1, SECONDS).get();
  }

  private static void wrongUsage() {
    // You should never go into situation where you have transformation of value to Void.
    // If you need to apply some side-effect, use 'andThen' method.
    // If you want to end the chain, ALWAYS use .consume()
    someIo("hello world").
      flatMap(result -> errorIo(new IOException("meh"))).
      alwaysWith(result -> {
        // Note: result is a Try
        System.out.println(result);
        return null;
      });
  }

  private static <T> ComposableFuture<T> someIo(final T initialValue) {
    return ComposableFutures.fromValue(initialValue);
  }

  private static <T> ComposableFuture<T> someDelayedIo(final T initialValue, final int timeMs) {
    return ComposableFutures.schedule(() -> initialValue, timeMs, MILLISECONDS);
  }

  private static <T> ComposableFuture<T> errorIo(final Exception exception) {
    return ComposableFutures.fromError(exception);
  }
}