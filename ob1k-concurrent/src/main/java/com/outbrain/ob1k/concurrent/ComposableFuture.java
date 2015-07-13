package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.handlers.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>A base interface for all future implementation in the system.
 * A composable future is a monadic construct that represents a (potentially long) computation that will eventually create a value or an error.
 * Futures are created from a Producer that represents the long computation.
 * Futures can be either eager or lazy.
 * Eager futures activate the producer immediately and stores the result(or error) internally.
 * Lazy futures stores the producer and activate it upon consumption</p>
 *
 * <p>Futures can be "continued" to create a flow of computations, each next step is activated upon the arrival of the result
 * on the previous future.</p>
 *
 * <p>Futures can be activated by calling the consumed method that activates the future(in the case of a lazy one)
 * or just "waits"(using a callback) for the result(in case of an eager one)</p>
 *
 * <p>For testing purposes or for usage inside a blocking context, the future result can be returned in a blocking manner
 * by calling the get method. however, in a non-blocking asynchronous environment such as Ob1k <b>it should never be used.</b></p>
 *
 * @author aronen
 * @since 6/6/13
 */
public interface ComposableFuture<T> {
  /**
   * Continues a future with a handler that will be called whether the future has resulted in a successful value or an error.
   *
   * @param handler the continuation handler that returns a future
   * @param <R> the resulting future type.
   * @return a new future that will produce the result from the handler.
   */
  <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler);

  /**
   * continues a future with a handler that will be called whether the future has resulted in a successful value or an error.
   *
   * @param handler the continuation handler that returns a value or throws an exception.
   * @param <R> the resulting future type.
   * @return a new future that will produce the result from the handler.
   */
  <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler);

  /**
   * continues a future with a handler that will be called only if the original future resulted with success
   * in case of an error the error is continues forward.
   *
   * @param handler the continuation handler that returns a future(a.k.a flatMap)
   * @param <R> the resulting future type.
   * @return a new future that will produce the result either from the handler if successful or the original error.
   */
  <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, R> handler);

  /**
   * continues a future with a handler that will be called only if the original future resulted with success
   * in case of an error the error is continued forward.
   *
   * @param handler the continuation handler that returns a future(a.k.a map)
   * @param <R> the resulting future type.
   * @return return a new future that will produce the result either from the handler if successful or the original error.
   */
  <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler);

  /**
   * continues a future with a handler that will be called only if the original future failed
   * in case of a success the original result is continued forward.
   *
   * @param handler the continuation handler that returns a future
   * @return a new future that will produce the original successful value the the result of the handler.
   */
  ComposableFuture<T> continueOnError(final FutureErrorHandler<T> handler);

  /**
   * continues a future with a handler that will be called only if the original future failed
   * in case of a success the original result is continued forward.
   *
   * @param handler the continuation handler that returns a value or throws an exception.
   * @return a new future that will produce the original successful value the the result of the handler.
   */
  ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler);

  /**
   * consumes the value(or error) of the future into a consumer.
   * if the future is lazy the value will be reproduced on each consumption.
   * if the future is eager the consumer will be served from the cached result.
   *
   * @param consumer the consumer.
   */
  void consume(Consumer<T> consumer);

  /**
   * blocks until a value is available for consumption and then return it.
   * in case of an error the exception is wrapped inside an ExecutionException and thrown.
   *
   * @return the future value if successful
   * @throws InterruptedException if the thread has been interrupted
   * @throws ExecutionException if the future return an error.
   */
  T get() throws InterruptedException, ExecutionException;

  /**
   * blocks until a value is available for consumption or until a timeout occurs, and then return the result or error.
   * in case of an error the exception is wrapped inside an ExecutionException and thrown.
   *
   * @param timeout max wait time for result.
   * @param unit a time unit for the timeout duration
   * @return the result if successful
   * @throws InterruptedException if the thread has been interrupted
   * @throws ExecutionException if the future return an error
   * @throws TimeoutException if result(or error) haven't arrived in the specified time-span.
   */
  T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

  /**
   * turns the current future into an eager one.
   * @return the new eager future.
   */
  ComposableFuture<T> materialize();

  /**
   * caps the max time for producing a value(or error) for this future.
   * the returned future will return the original result if available within the specified time or a TimeoutException.
   * @param duration max wait time for a result before producing a timeout
   * @param unit the duration timeout.
   * @return the future with a caped time.
   */
  ComposableFuture<T> withTimeout(long duration, final TimeUnit unit);
  ComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit);

  <R> ComposableFuture<R> transform(final Function<? super T, ? extends R> function);
}
