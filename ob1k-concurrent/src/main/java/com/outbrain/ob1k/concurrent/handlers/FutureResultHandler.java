package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Try;

/**
 * Created with IntelliJ IDEA.
 * User: aronen
 * Date: 6/6/13
 * Time: 2:01 PM
 */
public interface FutureResultHandler<T, R> {
  ComposableFuture<R> handle(Try<T> result);
}
