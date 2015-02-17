package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;

/**
 * represent either a result or an error using two ADTs - Success and Failure.
 *
 * @author aronen
 */
public abstract class Try<T> {
  // to prevent any further derivation.
  private Try() {}

  public abstract boolean isSuccess();
  public abstract T getValue();
  public abstract Throwable getError();
  public abstract <U> Try<U> map(Function<? super T, ? extends U> func);
  public abstract <U> Try<U> flatMap(Function<? super T, Try<U>> func);
  public abstract Try<T> recover(Function<Throwable, T> func);

  public static <T> Try<T> fromValue(final T value) {
    return new Success<>(value);
  }

  public static <T> Try<T> fromError(final Throwable error) {
    return new Failure<>(error);
  }

  public static class Success<T> extends Try<T> {
    private final T value;

    public Success(final T value) {
      this.value = value;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public T getValue() {
      return value;
    }

    @Override
    public Throwable getError() {
      return null;
    }

    @Override
    public <U> Try<U> map(final Function<? super T,? extends U> func) {
      try {
        return new Success<>(func.apply(value));
      } catch (final Exception e) {
        return new Failure<>(e);
      }
    }

    @Override
    public <U> Try<U> flatMap(final Function<? super T, Try<U>> func) {
      try {
        return func.apply(getValue());
      } catch (final Exception e) {
        return new Failure<>(e);
      }
    }


    @Override
    public Try<T> recover(final Function<Throwable, T> func) {
      return this;
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }
  }

  public static final class Failure<T> extends Try<T> {
    private final Throwable error;

    public Failure(final Throwable error) {
      this.error = error;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public T getValue() {
      return null;
    }

    @Override
    public Throwable getError() {
      return error;
    }

    @Override
    public <U> Try<U> map(final Function<? super T,? extends U> func) {
      return new Failure<>(this.getError());
    }

    @Override
    public <U> Try<U> flatMap(final Function<? super T, Try<U>> func) {
      return new Failure<>(this.getError());
    }

    @Override
    public Try<T> recover(final Function<Throwable, T> func) {
      try {
        return new Success<>(func.apply(error));
      } catch (final Exception e) {
        return new Failure<>(e);
      }
    }

    @Override
    public String toString() {
      return "Failure(" + error.toString() + ")";
    }
  }

}
