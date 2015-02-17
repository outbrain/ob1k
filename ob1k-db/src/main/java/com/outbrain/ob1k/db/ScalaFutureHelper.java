package com.outbrain.ob1k.db;

import scala.concurrent.Future;
import scala.util.Try;
import com.outbrain.ob1k.concurrent.*;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.runtime.AbstractFunction1;

/**
 * User: aronen
 * Date: 9/17/13
 * Time: 3:43 PM
 */
public class ScalaFutureHelper {
  public static final ExecutionContextExecutor ctx =
      ExecutionContext$.MODULE$.fromExecutor(ComposableFutures.getExecutor());

  public static interface FutureProvider<T> {
    scala.concurrent.Future<T> provide();
  }

  public static <T> ComposableFuture<T> from(final FutureProvider<T> source) {
    return ComposableFutures.build(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        final Future<T> future = source.provide();
        future.onComplete(new AbstractFunction1<Try<T>, Void>() {
          @Override public Void apply(final Try<T> res) {
            if (res.isSuccess()) {
              consumer.consume(com.outbrain.ob1k.concurrent.Try.fromValue(res.get()));
            } else {
              consumer.consume(com.outbrain.ob1k.concurrent.Try.<T>fromError(res.failed().get()));
            }

            return null;
          }

        }, ctx);
      }
    });
  }
}
