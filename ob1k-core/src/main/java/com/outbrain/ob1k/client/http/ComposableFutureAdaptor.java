package com.outbrain.ob1k.client.http;

import com.ning.http.client.ListenableFuture;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/16/13
 * Time: 4:16 PM
 */
public class ComposableFutureAdaptor {
  public static <T> ComposableFuture<T> fromListenableFuture(final ListenableFuture<T> source) {
    final ComposablePromise<T> res = ComposableFutures.newPromise(false);
    source.addListener(new Runnable() {
      @Override public void run() {
        try {
          final T result = source.get();
          res.set(result);
        } catch (final InterruptedException e) {
          res.setException(e);
        } catch (final ExecutionException e) {
          res.setException(e.getCause() != null ? e.getCause() : e);
        }
      }
    }, ComposableFutures.getExecutor());

    return res;
  }
}
