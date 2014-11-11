package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.handlers.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by aronen on 10/26/14.
 *
 * a fixed implementation of a Promise that already contains the final value.
 */
public class FulfilledFuture<T> implements ComposablePromise<T> {
  private final T value;
  private final Throwable error;

  public FulfilledFuture(final T value) {
    this.value = value;
    this.error = null;
  }

  public FulfilledFuture(final Throwable error) {
    this.error = error;
    this.value = null;
  }

  @Override
  public void set(final T value) {
    // should it perform as trySet ?
    throw new UnsupportedOperationException("already fulfilled future.");
  }

  @Override
  public void setException(final Throwable error) {
    // should it perform as trySetException ?
    throw new UnsupportedOperationException("already fulfilled future.");
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
    try {
      return handler.handle(this);
    } catch (final Exception e) {
      return new FulfilledFuture<>(e);
    }
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
    try {
      return ComposableFutures.fromValue(handler.handle(this));
    } catch (final ExecutionException e) {
      final Throwable cause = e.getCause() != null ? e.getCause() : e;
      return new FulfilledFuture<>(cause);
    } catch (final Exception e) {
      return new FulfilledFuture<>(e);
    }
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, ? extends R> handler) {
    if (isSuccess()) {
      try {
        return (ComposableFuture<R>) handler.handle(value);
      } catch (final Exception e) {
        return new FulfilledFuture<>(e);
      }
    } else {
      return (ComposableFuture<R>) this;
    }
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler) {
    if (isSuccess()) {
      try {
        return ComposableFutures.fromValue(handler.handle(value));
      } catch (final ExecutionException e) {
        final Throwable cause = e.getCause() != null ? e.getCause() : e;
        return new FulfilledFuture<>(cause);
      } catch (final Exception e) {
        return new FulfilledFuture<>(e);
      }
    } else {
      return (ComposableFuture<R>) this;
    }
  }

  @Override
  public ComposableFuture<T> continueOnError(final FutureErrorHandler<? extends T> handler) {
    if (isSuccess()) {
      return this;
    } else {
      try {
        return (ComposableFuture<T>) handler.handle(error);
      } catch (final Exception e) {
        return new FulfilledFuture<T>(e);
      }
    }
  }

  @Override
  public ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler) {
    if (isSuccess()) {
      return this;
    } else {
      try {
        return ComposableFutures.fromValue(handler.handle(error));
      } catch (final ExecutionException e) {
        final Throwable cause = e.getCause() != null ? e.getCause() : e;
        return new FulfilledFuture<>(cause);
      } catch (final Exception e) {
        return new FulfilledFuture<>(e);
      }
    }
  }

  @Override
  public void onResult(final OnResultHandler<T> handler) {
    handler.handle(this);
  }

  @Override
  public void onSuccess(final OnSuccessHandler<? super T> handler) {
    if (isSuccess()) {
      handler.handle(value);
    }
  }

  @Override
  public void onError(final OnErrorHandler handler) {
    if (!isSuccess()) {
      handler.handle(error);
    }
  }

  @Override
  public ComposableFuture<T> withTimeout(final long duration, final TimeUnit unit) {
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
  public boolean cancel(final boolean mayInterruptIfRunning) {
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
  public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return get();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof ComposableFuture)) return false;

    final ComposableFuture that = (ComposableFuture) o;
    final State thatState = that.getState();
    if (thatState == State.Success) {
      try {
        final Object thatValue = that.get();
        return value == null ? thatValue == null : value.equals(thatValue);
      } catch (final Exception e) {
        return false;
      }
    } else if (thatState == State.Failure) {
      return error.equals(that.getError());
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    if (getState() == State.Success) {
      return value != null ? value.hashCode() : 0;
    } else {
      return error.hashCode();
    }
  }
}
