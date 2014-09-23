package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.handlers.*;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 1:58 PM
 */
public interface ComposableFuture<T> extends Future<T> {
  <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler);
  <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler);

  <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, ? extends R> handler);
  <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler);

  ComposableFuture<T> continueOnError(final FutureErrorHandler<? extends T> handler);
  ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler);

  void onResult(OnResultHandler<T> handler);
  void onSuccess(OnSuccessHandler<? super T> handler);
  void onError(OnErrorHandler handler);

  ComposableFuture<T> withTimeout(long duration, final TimeUnit unit);
  <R> ComposableFuture<R> transform(final Function<? super T, ? extends R> function);

  public static enum State { Waiting, Success, Failure }

  State getState();
  boolean isSuccess();
  Throwable getError();
}
