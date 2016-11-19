package com.outbrain.ob1k.concurrent.eager;

import com.outbrain.ob1k.concurrent.CancellationToken;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Producer;
import com.outbrain.ob1k.concurrent.Scheduler;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.UncheckedExecutionException;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureAction;
import com.outbrain.ob1k.concurrent.handlers.FutureErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.ResultHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 2:08 PM
 */
public final class EagerComposableFuture<T> implements ComposableFuture<T>, ComposablePromise<T> {

  private static final Logger logger = LoggerFactory.getLogger(EagerComposableFuture.class);

  private final Executor threadPool;
  private final HandlersList handlers;
  private final AtomicReference<Try<T>> value = new AtomicReference<>();

  public EagerComposableFuture() {
    threadPool = null;
    handlers = new HandlersList();
  }

  public EagerComposableFuture(final Executor threadPool) {
    this.threadPool = threadPool;
    handlers = new HandlersList();
  }

  public static <T> ComposableFuture<T> fromValue(final T value) {
    final EagerComposableFuture<T> result = new EagerComposableFuture<>();
    result.set(value);
    return result;
  }

  public static <T> ComposableFuture<T> fromError(final Throwable error) {
    final EagerComposableFuture<T> result = new EagerComposableFuture<>();
    result.setException(error);
    return result;
  }

  public static <T> ComposableFuture<T> build(final Producer<? extends T> producer) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>();
    producer.produce(result -> {
      if (result.isSuccess()) {
        future.set(result.getValue());
      } else {
        future.setException(result.getError());
      }
    });

    return future;
  }

  public static <T> ComposableFuture<T> submit(final Executor executor, final Callable<T> task, final boolean delegateHandler) {
    if (task == null)
      return fromError(new NullPointerException("task must not be null"));

    final EagerComposableFuture<T> future = delegateHandler ?
      new EagerComposableFuture<>(executor) :
      new EagerComposableFuture<>();

    executor.execute(() -> future.setTry(Try.apply(task::call)));
    return future;
  }

  public static <T> ComposableFuture<T> schedule(final Scheduler scheduler, final Callable<T> task, final long delay, final TimeUnit unit) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>();
    scheduler.schedule(() -> future.setTry(Try.apply(task::call)), delay, unit);
    return future;
  }

  public static <T> ComposableFuture<T> doubleDispatch(final FutureAction<T> action, final long duration,
                                                       final TimeUnit unit, final Scheduler scheduler) {
    final ComposableFuture<T> first = action.execute();
    final AtomicBoolean done = new AtomicBoolean();

    first.consume(result -> done.compareAndSet(false, true));

    final EagerComposableFuture<T> second = new EagerComposableFuture<>();
    scheduler.schedule(() -> {
      if (done.compareAndSet(false, true)) {
        try {
          final ComposableFuture<T> innerSecond = action.execute();
          innerSecond.consume(result -> {
            if (result.isSuccess()) {
              second.set(result.getValue());
            } else {
              second.setException(result.getError());
            }
          });
        } catch (final Exception e) {
          second.setException(e);
        }
      }
    }, duration, unit);

    return collectFirst(Arrays.asList(first, second));
  }

  public static <T> ComposableFuture<T> collectFirst(final List<ComposableFuture<T>> futures) {
    final int size = futures.size();
    if (size == 0) {
      return fromError(new IllegalArgumentException("empty future list"));
    }

    final EagerComposableFuture<T> res = new EagerComposableFuture<>();
    final AtomicBoolean done = new AtomicBoolean();

    for (final ComposableFuture<T> future : futures) {
      future.consume(result -> {
        if (done.compareAndSet(false, true)) {
          if (result.isSuccess()) {
            res.set(result.getValue());
          } else {
            res.setException(result.getError());
          }
        }
      });
    }

    return res;
  }

  @Override
  public void setTry(final Try<? extends T> value) {
    if (value.isSuccess()) {
      set(value.getValue());
    } else {
      setException(value.getError());
    }
  }

  @Override
  public void set(final T result) {
    if (value.compareAndSet(null, Try.fromValue(result))) {
      done();
    }
  }

  @Override
  public void setException(final Throwable t) {
    if (value.compareAndSet(null, Try.fromError(t))) {
      done();
    }
  }

  @Override
  public ComposableFuture<T> future() {
    return this;
  }

  private void done() {
    handlers.execute(threadPool);
  }

  @Override
  public <R> ComposableFuture<R> map(final Function<? super T, ? extends R> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(result -> {
      if (result.isSuccess()) {
        try {
          future.set(handler.apply(result.getValue()));
        } catch (final UncheckedExecutionException e) {
          future.setException(e.getCause() != null ? e.getCause() : e);
        } catch (final Exception e) {
          future.setException(e);
        }
      } else {
        future.setException(result.getError());
      }
    });

    return future;
  }

  @Override
  public <R> ComposableFuture<R> flatMap(final Function<? super T, ? extends ComposableFuture<? extends R>> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(result -> {
      if (result.isSuccess()) {
        try {
          final ComposableFuture<? extends R> res = handler.apply(result.getValue());
          if (res == null) {
            future.set(null);
          } else {
            res.consume(futureResult -> {
              if (futureResult.isSuccess()) {
                future.set(futureResult.getValue());
              } else {
                future.setException(futureResult.getError());
              }
            });
          }

        } catch (final Exception e) {
          future.setException(e);
        }
      } else {
        future.setException(result.getError());
      }
    });

    return future;
  }

  @Override
  public ComposableFuture<T> recover(final Function<Throwable, ? extends T> handler) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>(threadPool);
    this.consume(result -> {
      if (result.isSuccess()) {
        future.set(result.getValue());
      } else {
        try {
          future.set(handler.apply(result.getError()));
        } catch (final UncheckedExecutionException e) {
          future.setException(e.getCause() != null ? e.getCause() : e);
        } catch (final Exception e) {
          future.setException(e);
        }
      }
    });

    return future;
  }

  @Override
  public ComposableFuture<T> recoverWith(final Function<Throwable, ? extends ComposableFuture<? extends T>> handler) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>(threadPool);
    this.consume(result -> {
      if (result.isSuccess()) {
        future.set(result.getValue());
      } else {
        try {
          final ComposableFuture<? extends T> res = handler.apply(result.getError());
          if (res == null) {
            future.set(null);
          } else {
            res.consume(futureResult -> {
              if (futureResult.isSuccess()) {
                future.set(futureResult.getValue());
              } else {
                future.setException(futureResult.getError());
              }
            });
          }

        } catch (final Exception e) {
          future.setException(e);
        }
      }
    });

    return future;
  }

  @Override
  public <R> ComposableFuture<R> always(final Function<Try<T>, ? extends R> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(res -> {
      try {
        future.set(handler.apply(res));
      } catch (final UncheckedExecutionException e) {
        future.setException(e.getCause() != null ? e.getCause() : e);
      } catch (final Exception e) {
        future.setException(e);
      }
    });

    return future;
  }

  @Override
  public <R> ComposableFuture<R> alwaysWith(final Function<Try<T>, ? extends ComposableFuture<? extends R>> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(res -> {
      try {
        final ComposableFuture<? extends R> nextResult = handler.apply(res);
        if (nextResult == null) {
          future.set(null);
        } else {
          nextResult.consume(result -> {
            if (result.isSuccess()) {
              future.set(result.getValue());
            } else {
              future.setException(result.getError());
            }
          });
        }

      } catch (final Exception e) {
        future.setException(e);
      }
    });

    return future;
  }

  @Override
  public ComposableFuture<T> andThen(final Consumer<? super T> resultConsumer) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>(threadPool);
    this.consume(result -> {
      resultConsumer.consume(result.map(identity()));
      future.setTry(result);
    });

    return future;
  }

  @Override
  public void consume(final Consumer<? super T> consumer) {
    handlers.addHandler(new ConsumerAction<>(consumer, this), threadPool);
  }

  @Override
  public ComposableFuture<T> withTimeout(final long duration, final TimeUnit unit, final String taskDescription) {
    return withTimeout(ComposableFutures.getScheduler(), duration, unit, taskDescription);
  }

  @Override
  public ComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit, final String taskDescription) {
    final ComposablePromise<T> deadline = new EagerComposableFuture<>();
    final CancellationToken cancellationToken = scheduler.schedule(() ->
      deadline.setException(new TimeoutException("Timeout occurred on task ('" + taskDescription + "' " + timeout + " " + unit + ")")), timeout, unit);

    this.consume(result -> cancellationToken.cancel(false));
    return collectFirst(Arrays.asList(this, deadline.future()));
  }

  @Override
  public ComposableFuture<T> withTimeout(final long duration, final TimeUnit unit) {
    return withTimeout(ComposableFutures.getScheduler(), duration, unit);
  }

  @Override
  public ComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit) {
    return withTimeout(scheduler, timeout, unit, "unspecified task");
  }

  @Override
  public ComposableFuture<T> materialize() {
    return this;
  }


  /*
    OLD API - DEPRECATED
 */


  @Override
  @SuppressWarnings({"unchecked", "deprecated"})
  public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
    return alwaysWith(handler::handle);
  }

  @Override
  @SuppressWarnings({"unchecked", "deprecated"})
  public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
    return always(result -> {
      try {
        return handler.handle(result);
      } catch (final ExecutionException e) {
        // new API doesn't allows checked exceptions - offering throwing runtime instead. sadly, doing this for BC.
        throw new UncheckedExecutionException(e.getCause());
      }
    });
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, R> handler) {
    return flatMap(handler::handle); // sweet
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler) {
    return map(result -> {
      try {
        return handler.handle(result);
      } catch (final ExecutionException e) {
        // new API doesn't allows checked exceptions - offering throwing runtime instead. sadly, doing this for BC.
        throw new UncheckedExecutionException(e.getCause());
      }
    });
  }

  @Override
  public ComposableFuture<T> continueOnError(final FutureErrorHandler<T> handler) {
    return recoverWith(handler::handle);
  }

  @Override
  public ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler) {
    return recover(error -> {
      try {
        return handler.handle(error);
      } catch (final ExecutionException e) {
        // new API doesn't allows checked exceptions - offering throwing runtime instead. sadly, doing this for BC.
        throw new UncheckedExecutionException(e.getCause());
      }
    });
  }

  @Override
  public <R> ComposableFuture<R> transform(final com.google.common.base.Function<? super T, ? extends R> function) {
    return continueOnSuccess(function::apply);
  }

  private static class ConsumerAction<T> implements Runnable {
    final Consumer<? super T> inner;
    final EagerComposableFuture<T> current;

    private ConsumerAction(final Consumer<? super T> inner, final EagerComposableFuture<T> current) {
      this.inner = inner;
      this.current = current;
    }

    @Override
    public void run() {
      try {
        final Try<T> currentValue = current.value.get();
        inner.consume(currentValue.map(identity()));
      } catch (final Throwable error) {
        logger.warn("error while handling future callbacks", error);
      }
    }
  }
}
