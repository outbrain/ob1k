package com.outbrain.ob1k.concurrent;

import com.outbrain.ob1k.concurrent.handlers.OnResultHandler;
import rx.Observable;
import rx.Subscriber;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by aronen on 10/27/14.
 */
public class FuturesToStreamHandler<T> implements Observable.OnSubscribe<T>, OnResultHandler<T> {
  private final List<ComposableFuture<T>> futures;
  private final List<Subscriber<? super T>> subscribers;
  private final AtomicInteger completed;
  private final boolean failOnError;
  private volatile boolean done = false;

  public FuturesToStreamHandler(final List<ComposableFuture<T>> futures, final boolean failOnError) {
    this.futures = futures;
    this.subscribers = new CopyOnWriteArrayList<>();
    this.completed = new AtomicInteger(0);
    this.failOnError = failOnError;

    for (final ComposableFuture<T> future : futures) {
      future.onResult(this);
    }
  }

  @Override
  public void call(final Subscriber<? super T> subscriber) {
    if (!done) {
      subscribers.add(subscriber);
    } else {
      subscriber.onCompleted();
    }
  }

  @Override
  public void handle(final ComposableFuture<T> result) {
    final boolean lastResult = completed.incrementAndGet() == futures.size();
    if (lastResult) {
      done = true;
    }

    if (result.isSuccess()) {
      for (final Subscriber<? super T> subscriber: subscribers) {
        try {
          subscriber.onNext(result.get());
        } catch (final Exception e) {
          // should never get here...
          subscriber.onError(e);
        }
        if (lastResult) {
          subscriber.onCompleted();
        }
      }
    } else if (failOnError) {
      done = true;
      for (final Subscriber<? super T> subscriber: subscribers) {
        subscriber.onError(result.getError());
      }
    } else if (lastResult) {
      for (final Subscriber<? super T> subscriber: subscribers) {
        subscriber.onCompleted();
      }
    }
  }
}
