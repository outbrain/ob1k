package com.outbrain.ob1k.concurrent;

/**
 * Created with IntelliJ IDEA.
 * User: aronen
 * Date: 6/10/13
 * Time: 5:25 PM
 */
public interface ComposablePromise<T> extends ComposableFuture<T> {
  void set(T value);
  void setException(Throwable error);
}
