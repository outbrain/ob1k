package com.outbrain.ob1k.concurrent;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.function.Function.identity;

/**
 * Represents a computation that may either result in an exception, or return a successfully computed value.
 * Similar to Scala's Try.
 *
 * @param <T> the type returned by the computation.
 * @author marenzon, aronen
 */
public abstract class Try<T> {

  /**
   * Creates new {@link Success} try from given value.
   *
   * @param value computation result
   * @param <T>   computation type
   * @return new {@link Success} Try
   */
  public static <T> Try<T> fromValue(final T value) {
    return Success.of(value);
  }

  /**
   * Creates new {@link Failure} Try from given exception.
   *
   * @param error computation error
   * @param <T>   computation type
   * @return new {@link Failure} Try
   */
  public static <T> Try<T> fromError(final Throwable error) {
    return Failure.of(error);
  }

  /**
   * Creates new either {@link Success} or {@link Failure} Try by given supplier result.
   * In case supplier throws exception, {@link Failure} Try will be returned.
   *
   * @param supplier computation value supplier
   * @param <T>      computation type
   * @return either {@link Success}, or {@link Failure} by supplier result
   */
  public static <T> Try<T> apply(final CheckedSupplier<? extends T> supplier) {
    try {
      return fromValue(supplier.get());
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  /**
   * Flats nested {@link Try} of {@link Try} into flatten one.
   *
   * @param nestedTry nested try to flatten
   * @param <U>       computation type
   * @return flatten Try
   */
  public static <U> Try<U> flatten(final Try<? extends Try<? extends U>> nestedTry) {
    if (nestedTry.isFailure()) {
      return fromError(nestedTry.getError());
    }

    return nestedTry.getValue().map(identity());
  }

  /**
   * @return true if the Try is a Success, or false in case of Failure.
   */
  public abstract boolean isSuccess();

  /**
   * @return true if the Try is a Failure, or false in case of Success.
   */
  public abstract boolean isFailure();

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
  public abstract <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> function);

  /**
   * Recovers a {@link Failure} Try into a {@link Success} one.
   *
   * @param recover a function to apply the exception
   * @return a new Success
   */
  public abstract Try<T> recover(Function<Throwable, ? extends T> recover);

  /**
   * Recovers a {@link Failure} Try into a {@link Success} one, applying only for matching
   * error type.
   *
   * @param type    error type to match
   * @param recover a function to apply the exception
   * @return a new Success
   */
  public abstract <E extends Throwable> Try<T> recover(Class<E> type, Function<E, ? extends T> recover);

  /**
   * Recovers a {@link Failure} Try into a new supplied Try.
   *
   * @param recover a function to apply the exception
   * @return a new Try
   */
  public abstract Try<T> recoverWith(Function<Throwable, ? extends Try<? extends T>> recover);

  /**
   * Recovers a {@link Failure} Try into a new supplied Try, applying only for matching
   * error type.
   *
   * @param type    error type to match
   * @param recover a function to apply the exception
   * @return a new Try
   */
  public abstract <E extends Throwable> Try<T> recoverWith(Class<E> type,
                                                           Function<E, ? extends Try<? extends T>> recover);

  /**
   * Applies mapper function in case of Success, or recover function in case of Failure.
   * Transforms current Try to a new one, depends on Try status.
   *
   * @param mapper  a function to apply for the Success value
   * @param recover a function to apply for the Failure exception
   * @param <U>     computation type
   * @return a new Try from applied functions
   */
  public abstract <U> Try<U> transform(Function<? super T, ? extends Try<? extends U>> mapper,
                                       Function<Throwable, ? extends Try<? extends U>> recover);

  /**
   * Feeds the value to a {@link Consumer} if {@link Try} is
   * a {@link Success}. If {@link Try} is a {@link Failure} it takes no action.
   *
   * @param consumer for the value of T
   */
  public abstract void forEach(Consumer<? super T> consumer);

  /**
   * Applies recover function in case of Failure or mapper function in case of Success.
   * If mapper is applied and throws an exception, then recover is applied with this exception.
   *
   * @param mapper  a function to apply for the Success value
   * @param recover a function to apply for the Failure exception
   * @param <U>     computation type
   * @return the result of applied functions
   */
  public abstract <U> Try<U> fold(Function<? super T, ? extends U> mapper,
                                  Function<Throwable, ? extends U> recover);

  /**
   * Ensures that the (successful) result of the current Try satisfies the given predicate,
   * or fails with the given value.
   *
   * @param predicate the predicate for the result
   * @return new future with same value if predicate returns true, else new future with a failure
   */
  public abstract Try<T> ensure(final Predicate<? super T> predicate);

  /**
   * @return the value if computation succeed, or throws the exception of the error.
   * @throws Throwable computation error
   */
  public abstract T get() throws Throwable;

  /**
   * @return the computed value, else null.
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
  public abstract T getOrElse(Supplier<? extends T> defaultValue);

  /**
   * @param defaultTry the default try to return if Try is Failure.
   * @return the try if Success, else defaultTry.
   */
  public abstract Try<T> orElse(Supplier<? extends Try<T>> defaultTry);

  /**
   * @return {@link Optional} of the current Try
   */
  public abstract Optional<T> toOptional();

  public static final class Success<T> extends Try<T> {

    private final T value;

    public Success(final T value) {
      this.value = value;
    }

    public static <T> Success<T> of(final T value) {
      return new Success<>(value);
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
    public T getOrElse(final Supplier<? extends T> defaultValue) {
      return value;
    }

    @Override
    public Try<T> orElse(final Supplier<? extends Try<T>> defaultTry) {
      return this;
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.ofNullable(value);
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
    public <U> Try<U> fold(final Function<? super T, ? extends U> mapper,
                           final Function<Throwable, ? extends U> recover) {
      final Try<U> mappedValue = apply(() -> mapper.apply(value));
      return mappedValue.recover(recover::apply);
    }

    @Override
    public Try<T> ensure(final Predicate<? super T> predicate) {
      if (predicate.test(value)) {
        return fromValue(value);
      }

      return fromError(new NoSuchElementException("predicate is not satisfied"));
    }

    @Override
    public <U> Try<U> flatMap(final Function<? super T, ? extends Try<? extends U>> func) {
      return flatten(apply(() -> func.apply(value)));
    }

    @Override
    public Try<T> recover(final Function<Throwable, ? extends T> function) {
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> recover(final Class<E> type, final Function<E, ? extends T> recover) {
      return this;
    }

    @Override
    public Try<T> recoverWith(final Function<Throwable, ? extends Try<? extends T>> function) {
      return this;
    }

    @Override
    public <E extends Throwable> Try<T> recoverWith(final Class<E> type,
                                                    final Function<E, ? extends Try<? extends T>> recover) {
      return this;
    }

    @Override
    public <U> Try<U> transform(final Function<? super T, ? extends Try<? extends U>> mapper,
                                final Function<Throwable, ? extends Try<? extends U>> recover) {
      return mapper.apply(value).map(identity());
    }

    @Override
    public void forEach(final Consumer<? super T> consumer) {
      consumer.accept(value);
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

    public static <T> Failure<T> of(final Throwable error) {
      return new Failure<>(error);
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
    public T getOrElse(final Supplier<? extends T> defaultValue) {
      return defaultValue.get();
    }

    @Override
    public Try<T> orElse(final Supplier<? extends Try<T>> defaultTry) {
      return defaultTry.get();
    }

    @Override
    public Optional<T> toOptional() {
      return Optional.empty();
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
    public <U> Try<U> flatMap(final Function<? super T, ? extends Try<? extends U>> function) {
      return fromError(error);
    }

    @Override
    public Try<T> recover(final Function<Throwable, ? extends T> recover) {
      return apply(() -> recover.apply(error));
    }

    @Override
    public <E extends Throwable> Try<T> recover(final Class<E> type, final Function<E, ? extends T> recover) {
      if (type.isInstance(error)) {
        final E matchedError = type.cast(error);
        return apply(() -> recover.apply(matchedError));
      }

      return this;
    }

    @Override
    public Try<T> recoverWith(final Function<Throwable, ? extends Try<? extends T>> function) {
      return flatten(apply(() -> function.apply(error)));
    }

    @Override
    public <E extends Throwable> Try<T> recoverWith(final Class<E> type,
                                                    final Function<E, ? extends Try<? extends T>> recover) {
      if (type.isInstance(error)) {
        final E matchedError = type.cast(error);
        return flatten(apply(() -> recover.apply(matchedError)));
      }

      return this;
    }

    @Override
    public <U> Try<U> transform(final Function<? super T, ? extends Try<? extends U>> mapper,
                                final Function<Throwable, ? extends Try<? extends U>> recover) {
      return recover.apply(error).map(identity());
    }

    @Override
    public <U> Try<U> fold(final Function<? super T, ? extends U> mapper,
                           final Function<Throwable, ? extends U> recover) {
      return apply(() -> recover.apply(error));
    }

    @Override
    public Try<T> ensure(final Predicate<? super T> predicate) {
      return this;
    }

    @Override
    public void forEach(final Consumer<? super T> consumer) {

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
