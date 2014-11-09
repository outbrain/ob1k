package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.handlers.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by aronen on 10/26/14.
 */
public class FulfilledFuture<T> implements ComposablePromise<T> {
  private final T value;
  private final Throwable error;

  public FulfilledFuture(T value) {
    this.value = value;
    this.error = null;
  }

  public FulfilledFuture(Throwable error) {
    this.error = error;
    this.value = null;
  }

  @Override
  public void set(T value) {
    // should is perform as trySet ?
    throw new UnsupportedOperationException("already fulfilled future.");
  }

  @Override
  public void setException(Throwable error) {
    // should is perform as trySetException ?
    throw new UnsupportedOperationException("already fulfilled future.");
  }

  @Override
  public <R> ComposableFuture<R> continueWith(FutureResultHandler<T, R> handler) {
    try {
      return handler.handle(this);
    } catch (Exception e) {
      return new FulfilledFuture<>(e);
    }
  }

  @Override
  public <R> ComposableFuture<R> continueWith(ResultHandler<T, R> handler) {
    try {
      return ComposableFutures.fromValue(handler.handle(this));
    } catch (ExecutionException e) {
      Throwable cause = e.getCause() != null ? e.getCause() : e;
      return new FulfilledFuture<>(cause);
    } catch (Exception e) {
      return new FulfilledFuture<>(e);
    }
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(FutureSuccessHandler<? super T, ? extends R> handler) {
    if (isSuccess()) {
      try {
        return (ComposableFuture<R>) handler.handle(value);
      } catch (Exception e) {
        return new FulfilledFuture<>(e);
      }
    } else {
      return (ComposableFuture<R>) this;
    }
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(SuccessHandler<? super T, ? extends R> handler) {
    if (isSuccess()) {
      try {
        return ComposableFutures.fromValue(handler.handle(value));
      } catch (ExecutionException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return new FulfilledFuture<R>(cause);
      } catch (Exception e) {
        return new FulfilledFuture<R>(e);
      }
    } else {
      return (ComposableFuture<R>) this;
    }
  }

  @Override
  public ComposableFuture<T> continueOnError(FutureErrorHandler<? extends T> handler) {
    if (isSuccess()) {
      return this;
    } else {
      try {
        return (ComposableFuture<T>) handler.handle(error);
      } catch (Exception e) {
        return new FulfilledFuture<T>(e);
      }
    }
  }

  @Override
  public ComposableFuture<T> continueOnError(ErrorHandler<? extends T> handler) {
    if (isSuccess()) {
      return this;
    } else {
      try {
        return ComposableFutures.fromValue(handler.handle(error));
      } catch (ExecutionException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        return new FulfilledFuture<>(cause);
      } catch (Exception e) {
        return new FulfilledFuture<>(e);
      }
    }
  }

  @Override
  public void onResult(OnResultHandler<T> handler) {
    handler.handle(this);
  }

  @Override
  public void onSuccess(OnSuccessHandler<? super T> handler) {
    if (isSuccess()) {
      handler.handle(value);
    }
  }

  @Override
  public void onError(OnErrorHandler handler) {
    if (!isSuccess()) {
      handler.handle(error);
    }
  }

  @Override
  public ComposableFuture<T> withTimeout(long duration, TimeUnit unit) {
    // already fulfilled.
    return this;
  }

  @Override
  public <R> ComposableFuture<R> transform(final Function<? super T, ? extends R> function) {
    return continueOnSuccess(new SuccessHandler<T, R>() {
      @Override
      public R handle(final T result) throws ExecutionException {
        return function.apply(result);
      }
    });
  }

  @Override
  public State getState() {
    return isSuccess() ? State.Success : State.Failure;
  }

  @Override
  public boolean isSuccess() {
    return error == null;
  }

  @Override
  public Throwable getError() {
    return error;
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    if (isSuccess()) {
      return value;
    }

    throw new ExecutionException(error);
  }

  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }
}
