package com.outbrain.ob1k.client.http;

import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/16/13
 * Time: 4:16 PM
 */
public class ComposableFutureAdaptor {
    public static interface ListenableFutureProvider<T> {
        ListenableFuture<T> provide();
    }

  public static <T> ComposableFuture<T> fromListenableFuture(final ListenableFutureProvider<T> provider, final Request request, final MetricFactory metricFactory) {
    return ComposableFutures.build(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
          final Counter counter;
          if (metricFactory != null) {
              final String host = request.getUri().getHost();
              counter = metricFactory.createCounter("HttpClient", host);

              counter.inc();
          } else {
              counter = null;
          }

          final ListenableFuture<T> source = provider.provide();
          source.addListener(new Runnable() {
              @Override
              public void run() {
                  try {
                      final T result = source.get();
                      consumer.consume(Try.fromValue(result));
                  } catch (final InterruptedException e) {
                      consumer.consume(Try.<T>fromError(e));
                  } catch (final ExecutionException e) {
                      final Throwable error = e.getCause() != null ? e.getCause() : e;
                      consumer.consume(Try.<T>fromError(error));
                  } finally {
                      if (counter != null) {
                          counter.dec();
                      }
                  }
              }
          }, ComposableFutures.getExecutor());
      }
    });
  }
}
