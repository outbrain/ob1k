package com.outbrain.ob1k.db;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import scala.concurrent.ExecutionContext$;
import scala.concurrent.ExecutionContextExecutor;
import scala.runtime.AbstractFunction1;
import scala.util.Try;

/**
 * User: aronen
 * Date: 9/17/13
 * Time: 3:43 PM
 */
public class ScalaFutureHelper {
  public static final ExecutionContextExecutor ctx =
      ExecutionContext$.MODULE$.fromExecutor(ComposableFutures.getExecutor());

  public static <T> ComposableFuture<T> from(scala.concurrent.Future<T> source) {
    final ComposablePromise<T> promise = ComposableFutures.newPromise();

    source.onComplete(new AbstractFunction1<Try<T>, Void>() {
      @Override public Void apply(Try<T> res) {
        if (res.isSuccess()) {
          promise.set(res.get());
          return null;
        } else {
          promise.setException(res.failed().get());
          return null;
        }
      }

    }, ctx);

    return promise;
  }
}
