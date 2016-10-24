package com.outbrain.ob1k.http.utils;

import com.ning.http.client.ListenableFuture;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import java.util.concurrent.ExecutionException;

public class ComposableFutureAdapter {

  public interface Provider<T> {

    ListenableFuture<T> provide();
  }

  public static <T> ComposableFuture<T> fromListenableFuture(final Provider<T> provider) {

    return ComposableFutures.build(consumer -> {

      final ListenableFuture<T> source = provider.provide();
      source.addListener(() -> {
        try {
          final T result = source.get();
          consumer.consume(Try.fromValue(result));
        } catch (final InterruptedException e) {
          consumer.consume(Try.fromError(e));
        } catch (final ExecutionException e) {
          final Throwable error = e.getCause() != null ? e.getCause() : e;
          consumer.consume(Try.fromError(error));
        }
      }, ComposableFutures.getExecutor());
    });
  }
}