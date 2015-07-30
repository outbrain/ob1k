package com.outbrain.ob1k.common.concurrent;

import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * @author marenzon
 */
public class ComposableFutureHelper {

  public static boolean isComposableFuture(final Class<?> type) {
    return type == ComposableFuture.class || type == com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture.class;
  }

  public static <T> ComposableFuture<T> cast(final Object object) {
    if (object instanceof ComposableFuture) {
      return (ComposableFuture<T>) object;
    }

    if (object instanceof com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture) {
      final com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture scalaFuture = (com.outbrain.ob1k.concurrent.scalaapi.ComposableFuture) object;
      return scalaFuture.asJavaComposableFuture();
    }

    throw new ClassCastException("cannot cast object to ComposableFuture");
  }
}