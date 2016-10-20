package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;

import java.util.Objects;

/**
 * Represents a computation that may either result in an exception, or return a successfully computed value.
 * Implements API similar to Scala's Try.
 *
 * @param <T> the type returned by the computation.
 * @author marenzon, aronen
 */
public abstract class Try<T> {

  public static <T> Try<T> fromValue(final T value) {
    return new Success<>(value);
  }

  public static <T> Try<T> fromError(final Throwable error) {
    return new Failure<>(error);
  }

  public static <T> Try<T> fromNull() {
    return fromValue(null);
  }

  public static <T> Try<T> apply(final CheckedSupplier<T> supplier) {
    try {
      return fromValue(supplier.get());
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  public static <U> Try<U> flatten(final Try<Try<U>> nestedTry) {
    if (nestedTry.isFailure()) {
      return fromError(nestedTry.getError());
    }
    return nestedTry.getValue();
  }

  /**
   * @return true if the Try is a Success, or false if it's a Failure.
   */
  public abstract boolean isSuccess();

  /**
   * @return true if the Try is a Failure, or false if it's a Success.
   */
  public abstract boolean isFailure();

  /**
   * @return the computed value.
   */
  public abstract T getValue();

  /**
   * @return the error if computation failed, else null.
   */
  public abstract Throwable getError();

  /**
   * @param defaultValue the default value to return if Try is Failure.
   * @return the value if Success, else defaultValue.
   */
  public abstract T getOrElse(T defaultValue);

  /**
   * @param defaultTry the default try to return if Try is Failure.
   * @return the try if Success, else defaultTry.
   */
  public abstract Try<T> orElse(Try<T> defaultTry);

  /**
   * @return the value if computation succeed, or throws the exception of the error.
   * @throws Throwable
   */
  public abstract T get() throws Throwable;

  /**
   * Maps the value of T to the value of type U.
   *
   * @param function a function to apply to the value of T.
   * @param <U>      the type of the result.
   * @return result of mapped value in a Try.
   */
  public abstract <U> Try<U> map(Function<? super T, ? extends U> function);

  /**
   * Maps the value of T to a new Try of U.
   *
   * @param function a function to apply to the value of T.
   * @param <U>      the type of the result.
   * @return a new Try of the mapped T value.
   */
  public abstract <U> Try<U> flatMap(Function<? super T, Try<U>> function);

  public abstract Try<T> recover(Function<Throwable, T> function);

  public abstract Try<T> recoverWith(Function<Throwable, Try<T>> function);

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
    public boolean isFailure() {
      return false;
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
    public T getOrElse(final T defaultValue) {
      return value;
    }

    @Override
    public Try<T> orElse(final Try<T> defaultTry) {
      return this;
    }

    @Override
    public T get() throws Throwable {
      return value;
    }

    @Override
    public <U> Try<U> map(final Function<? super T, ? extends U> func) {
      return apply(() -> func.apply(value));
    }

    @Override
    public <U> Try<U> flatMap(final Function<? super T, Try<U>> func) {
      return flatten(apply(() -> func.apply(getValue())));
    }

    @Override
    public Try<T> recover(final Function<Throwable, T> function) {
      return this;
    }

    @Override
    public Try<T> recoverWith(final Function<Throwable, Try<T>> function) {
      return this;
    }

    @Override
    public String toString() {
      return "Success(" + value + ")";
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Success<?> success = (Success<?>) o;
      return Objects.equals(value, success.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
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
    public boolean isFailure() {
      return true;
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
    public T getOrElse(final T defaultValue) {
      return defaultValue;
    }

    @Override
    public Try<T> orElse(final Try<T> defaultTry) {
      return defaultTry;
    }

    @Override
    public T get() throws Throwable {
      throw error;
    }

    @Override
    public <U> Try<U> map(final Function<? super T, ? extends U> function) {
      return fromError(error);
    }

    @Override
    public <U> Try<U> flatMap(final Function<? super T, Try<U>> function) {
      return fromError(error);
    }

    @Override
    public Try<T> recover(final Function<Throwable, T> function) {
      return apply(() -> function.apply(error));
    }

    @Override
    public Try<T> recoverWith(final Function<Throwable, Try<T>> function) {
      return flatten(apply(() -> function.apply(error)));
    }

    @Override
    public String toString() {
      return "Failure(" + error.toString() + ")";
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Failure<?> failure = (Failure<?>) o;
      return Objects.equals(error, failure.error);
    }

    @Override
    public int hashCode() {
      return Objects.hash(error);
    }
  }
}
