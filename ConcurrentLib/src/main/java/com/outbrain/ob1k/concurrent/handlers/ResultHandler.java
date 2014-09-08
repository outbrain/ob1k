package com.outbrain.ob1k.concurrent.handlers;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 2:05 PM
 */
public interface ResultHandler<T, R> {
  R handle(ComposableFuture<T> result) throws ExecutionException;
}
