package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Try;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 2:05 PM
 */
public interface ResultHandler<T, R> {
  R handle(Try<T> result) throws ExecutionException;
}
