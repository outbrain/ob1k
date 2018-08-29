package com.outbrain.ob1k.db.experimental;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;

import java.util.concurrent.CompletableFuture;

/**
 * User: aronen
 * Date: 9/17/13
 * Time: 3:43 PM
 */
public class JavaFutureHelper {

  public interface FutureProvider<T> {
    CompletableFuture<T> provide();
  }

  public static <T> ComposableFuture<T> from(final FutureProvider<T> source) {
    return ComposableFutures.build(consumer -> {
      final CompletableFuture<T> future = source.provide();
      future.whenCompleteAsync((v, t) -> {
        if (t == null) {
          consumer.consume(Try.fromValue(v));
        } else {
          consumer.consume(Try.fromError(t));
        }
      }, ComposableFutures.getExecutor());
    });
  }
}
