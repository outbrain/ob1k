package com.outbrain.ob1k.concurrent.stream;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import rx.Observable;
import rx.Subscriber;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * translates a set of futures into a stream.
 * each future produces a single value into the stream or ends it with an error.
 *
 * @author aronen
 */
public class FuturesToStreamHandler<T> implements Observable.OnSubscribe<T>, Consumer<T> {
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
      future.consume(this);
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
  public void consume(final Try<T> result) {
    final boolean lastResult = completed.incrementAndGet() == futures.size();
    if (lastResult) {
      done = true;
    }

    if (result.isSuccess()) {
      for (final Subscriber<? super T> subscriber: subscribers) {
        try {
          subscriber.onNext(result.getValue());
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
