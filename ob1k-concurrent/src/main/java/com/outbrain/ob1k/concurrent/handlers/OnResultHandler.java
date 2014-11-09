package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/11/13
 * Time: 7:52 PM
 */
public interface OnResultHandler<T> {
  void handle(ComposableFuture<T> result);
}
