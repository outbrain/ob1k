package com.outbrain.ob1k.concurrent.stream;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureProvider;
import rx.Observable;
import rx.Subscriber;

/**
 * Created by aronen on 11/9/14.
 *
 * creates a stream from a future provider.
 * each time a future provides the value, the next future is generated.
 */
public class FutureProviderToStreamHandler<T> implements Observable.OnSubscribe<T> {
  private final FutureProvider<T> provider;
  volatile Subscriber<? super T> subscriber;

  public FutureProviderToStreamHandler(final FutureProvider<T> provider) {
    this.provider = provider;
  }

  @Override
  public void call(final Subscriber<? super T> subscriber) {
    this.subscriber = subscriber;
    final boolean next = provider.moveNext();
    if (next) {
      final ComposableFuture<T> nextFuture = provider.current();
      handleNextFuture(nextFuture);
    } else {
      subscriber.onCompleted();
    }
  }

  private void handleNextFuture(final ComposableFuture<T> nextFuture) {
    nextFuture.consume(result -> {
      if (result.isSuccess()) {
        subscriber.onNext(result.getValue());
        final boolean next = provider.moveNext();
        if (next) {
          handleNextFuture(provider.current());
        } else {
          subscriber.onCompleted();
        }
      } else {
        subscriber.onError(result.getError());
      }
    });
 }

}