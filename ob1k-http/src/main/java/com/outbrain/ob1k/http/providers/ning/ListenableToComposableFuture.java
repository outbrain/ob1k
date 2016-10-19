package com.outbrain.ob1k.http.providers.ning;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.asynchttpclient.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static com.outbrain.ob1k.concurrent.ComposableFutures.build;
import static com.outbrain.ob1k.concurrent.Try.fromError;
import static com.outbrain.ob1k.concurrent.Try.fromValue;

class ListenableToComposableFuture {

  static <T> ComposableFuture<T> transform(final Supplier<ListenableFuture<T>> supplier) {
    return build(consumer -> {
      final ListenableFuture<T> source = supplier.get();
      source.addListener(() -> {
        try {
          final T result = source.get();
          consumer.consume(fromValue(result));
        } catch (final InterruptedException e) {
          consumer.consume(fromError(e));
        } catch (final ExecutionException e) {
          final Throwable error = e.getCause() != null ? e.getCause() : e;
          consumer.consume(fromError(error));
        }
      }, ComposableFutures.getExecutor());
    });
  }
}