package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.function.Function;

/**
 * User: aronen
 * Date: 11/3/13
 * Time: 2:30 PM
 */
public interface FutureSuccessHandler<T, R> {

  ComposableFuture<R> handle(T result);
}
