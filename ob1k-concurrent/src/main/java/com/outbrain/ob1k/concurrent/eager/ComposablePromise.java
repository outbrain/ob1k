package com.outbrain.ob1k.concurrent.eager;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Try;

/**
 * a promise is the producing side of an eager future.
 * it can be used to set a value or an error inside a future
 * that is returned from the promise.
 *
 * @author aronen
 * Date: 6/10/13
 * Time: 5:25 PM
 */
public interface ComposablePromise<T> {

  /**
   * set a value or error inside the corresponding future.
   * a value(or an error) can only be set once.
   *
   * @param value the value
   */
  void setTry(Try<? extends T> value);

  /**
   * set a value inside the corresponding future.
   * a value(or an error) can only be set once.
   *
   * @param value the value
   */
  void set(T value);

  /**
   * set an error inside the corresponding future.
   * an error(or a value) can only be set once.
   *
   * @param error the error.
   */
  void setException(Throwable error);

  /**
   * returns the corresponding future(always the same one)
   *
   * @return the future.
   */
  ComposableFuture<T> future();
}
