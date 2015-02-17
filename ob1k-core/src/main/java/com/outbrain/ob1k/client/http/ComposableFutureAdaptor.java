package com.outbrain.ob1k.client.http;

import com.ning.http.client.ListenableFuture;
import com.outbrain.ob1k.concurrent.*;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/16/13
 * Time: 4:16 PM
 */
public class ComposableFutureAdaptor {
  public static <T> ComposableFuture<T> fromListenableFuture(final ListenableFuture<T> source) {
    return ComposableFutures.build(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        source.addListener(new Runnable() {
          @Override public void run() {
            try {
              final T result = source.get();
              consumer.consume(Try.fromValue(result));
            } catch (final InterruptedException e) {
              consumer.consume(Try.<T>fromError(e));
            } catch (final ExecutionException e) {
              final Throwable error = e.getCause() != null ? e.getCause() : e;
              consumer.consume(Try.<T>fromError(error));
            }
          }
        }, ComposableFutures.getExecutor());
      }
    });
  }
}
