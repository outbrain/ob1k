package com.outbrain.ob1k.concurrent.eager;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.handlers.*;
import com.outbrain.ob1k.concurrent.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

  @Override
  public void set(final T result) {
    if (value.compareAndSet(null, Try.fromValue(result))) {
      done();
    }
  }

  @Override
  public void setException(final Throwable t) {
    if (value.compareAndSet(null, Try.<T>fromError(t))) {
      done();
    }
  }

  @Override
  public ComposableFuture<T> future() {
    return this;
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

  public static <T> ComposableFuture<T> build(final Producer<T> producer) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>();
    producer.produce(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        if (result.isSuccess()) {
          future.set(result.getValue());
        } else {
          future.setException(result.getError());
        }
      }
    });

    return future;
  }

  public static <T> ComposableFuture<T> submit(final Executor executor, final Callable<T> task, final boolean delegateHandler) {
    if (task == null)
      return fromError(new NullPointerException("task must not be null"));

    final EagerComposableFuture<T> future = delegateHandler ?
        new EagerComposableFuture<T>(executor) :
        new EagerComposableFuture<T>();

    executor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          future.set(task.call());
        } catch (final Exception e) {
          future.setException(e);
        }
      }
    });

    return future;
  }

  public static <T> ComposableFuture<T> schedule(final Scheduler scheduler, final Callable<T> task, final long delay, final TimeUnit unit) {
    final EagerComposableFuture<T> res = new EagerComposableFuture<>();
    scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        try {
          res.set(task.call());
        } catch (final Exception e) {
          res.setException(e);
        }
      }
    }, delay, unit);

    return res;
  }


  public static <T> ComposableFuture<T> doubleDispatch(final FutureAction<T> action, final long duration,
                                                       final TimeUnit unit, final Scheduler scheduler) {
    final ComposableFuture<T> first = action.execute();
    final AtomicBoolean done = new AtomicBoolean();

    first.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        done.compareAndSet(false, true);
      }
    });

    final EagerComposableFuture<T> second = new EagerComposableFuture<>();
    scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        if (done.compareAndSet(false, true)) {
          try {
            final ComposableFuture<T> innerSecond = action.execute();
            innerSecond.consume(new Consumer<T>() {
              @Override
              public void consume(final Try<T> result) {
                if (result.isSuccess()) {
                  second.set(result.getValue());
                } else {
                  second.setException(result.getError());
                }
              }
            });
          } catch (final Exception e) {
            second.setException(e);
          }
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
      future.consume(new Consumer<T>() {
        @Override
        public void consume(final Try<T> result) {
          if (done.compareAndSet(false, true)) {
            if (result.isSuccess()) {
              res.set(result.getValue());
            } else {
              res.setException(result.getError());
            }
          }
        }
      });
    }

    return res;
  }

  private void done() {
    handlers.execute(threadPool);
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> res) {
        try {
          final ComposableFuture<R> nextResult = handler.handle(res);
          if (nextResult == null) {
            future.set(null);
          } else {
            nextResult.consume(new Consumer<R>() {
              @Override
              public void consume(final Try<R> result) {
                if (result.isSuccess()) {
                  future.set(result.getValue());
                } else {
                  future.setException(result.getError());
                }
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
  public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
    final EagerComposableFuture<R> result = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> res) {
        try {
          result.set(handler.handle(res));
        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    return result;
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, R> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        if (result.isSuccess()) {
          try {
            final ComposableFuture<R> res = handler.handle(result.getValue());
            if (res == null) {
              future.set(null);
            } else {
              res.consume(new Consumer<R>() {
                @Override
                public void consume(final Try<R> result) {
                  if (result.isSuccess()) {
                    future.set(result.getValue());
                  } else {
                    future.setException(result.getError());
                  }
                }
              });
            }

          } catch (final Exception e) {
            future.setException(e);
          }
        } else {
          future.setException(result.getError());
        }
      }
    });

    return future;
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler) {
    final EagerComposableFuture<R> future = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        if (result.isSuccess()) {
          try {
            future.set(handler.handle(result.getValue()));
          } catch (final ExecutionException e) {
            future.setException(e.getCause() != null ? e.getCause() : e);
          } catch (final Exception e) {
            future.setException(e);
          }
        } else {
          future.setException(result.getError());
        }
      }
    });

    return future;
  }

  @Override
  public ComposableFuture<T> continueOnError(final FutureErrorHandler<T> handler) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        if (result.isSuccess()) {
          future.set(result.getValue());
        } else {
          try {
            final ComposableFuture<T> res = handler.handle(result.getError());
            if (res == null) {
              future.set(null);
            } else {
              res.consume(new Consumer<T>() {
                @Override
                public void consume(final Try<T> result) {
                  if (result.isSuccess()) {
                    future.set(result.getValue());
                  } else {
                    future.setException(result.getError());
                  }
                }
              });
            }

          } catch (final Exception e) {
            future.setException(e);
          }
        }
      }
    });

    return future;
  }

  @Override
  public ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler) {
    final EagerComposableFuture<T> future = new EagerComposableFuture<>(threadPool);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        if (result.isSuccess()) {
          future.set(result.getValue());
        } else {
          try {
            future.set(handler.handle(result.getError()));
          } catch (final ExecutionException e) {
            future.setException(e.getCause() != null ? e.getCause() : e);
          } catch (final Exception e) {
            future.setException(e);
          }
        }
      }
    });

    return future;
  }

  @Override
  public void consume(final Consumer<T> consumer) {
    handlers.addHandler(new ConsumerAction<>(consumer, this), threadPool);
  }

  @Override
  public <R> ComposableFuture<R> transform(final Function<? super T, ? extends R> function) {
    return continueOnSuccess(new SuccessHandler<T, R>() {
      @Override
      public R handle(final T result) {
        return function.apply(result);
      }
    });
  }

  @Override
  public ComposableFuture<T> withTimeout(final long duration, final TimeUnit unit, final String taskDescription) {
    return withTimeout(ComposableFutures.getScheduler(), duration, unit, taskDescription);
  }

  @Override
  public ComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit, final String taskDescription) {
    final ComposablePromise<T> deadline = new EagerComposableFuture<>();
    final CancellationToken cancellationToken =  scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        deadline.setException(new TimeoutException("Timeout occurred on task ('" + taskDescription + "' " + timeout + " " + unit + ")"));
      }
    }, timeout, unit);

    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        cancellationToken.cancel(false);
      }
    });
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

  @Override
  public T get() throws InterruptedException, ExecutionException {
    final CountDownLatch latch = new CountDownLatch(1);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        latch.countDown();
      }
    });

    latch.await();
    final Try<T> currentValue = this.value.get();
    if (currentValue.isSuccess()) {
      return currentValue.getValue();
    } else {
      throw new ExecutionException(currentValue.getError());
    }
  }

  @Override
  public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    final CountDownLatch latch = new CountDownLatch(1);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        latch.countDown();
      }
    });

    if (latch.await(timeout, unit)) {
      final Try<T> currentValue = this.value.get();
      if (currentValue.isSuccess()) {
        return currentValue.getValue();
      } else {
        throw new ExecutionException(currentValue.getError());
      }
    } else {
      throw new TimeoutException("Timeout occurred while waiting for value (" + timeout + unit + ")");
    }
  }

  private static class ConsumerAction<T> implements Runnable {
    final Consumer<T> inner;
    final EagerComposableFuture<T> current;

    private ConsumerAction(final Consumer<T> inner, final EagerComposableFuture<T> current) {
      this.inner = inner;
      this.current = current;
    }

    @Override
    public void run() {
      try {
        final Try<T> currentValue = current.value.get();
        inner.consume(currentValue);
      } catch (final Throwable error) {
        logger.warn("error while handling future callbacks", error);
      }
    }
  }



}
