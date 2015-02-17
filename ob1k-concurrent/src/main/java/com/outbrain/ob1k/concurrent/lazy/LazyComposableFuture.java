package com.outbrain.ob1k.concurrent.lazy;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.handlers.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * a future that contains a producer. each time the future is consumed the producer is activated
 * to deliver a value or an error to the consumer.
 *
 * the final result is never stored so the future is actually stateless as opposed to the eager future
 * that eventually holds the final result.
 *
 * the lazy future represent a computation that eventually creates a new value. that value can be consumed many times
 * by calling consume and supplying a consumer.
 *
 * @author asy ronen
 */
public final class LazyComposableFuture<T> implements ComposableFuture<T> {
  private final Producer<T> producer;
  private final Executor executor;

  private LazyComposableFuture(final Producer<T> producer) {
    this(producer, null);
  }

  private LazyComposableFuture(final Producer<T> producer, final Executor executor) {
    this.producer = producer;
    this.executor = executor;
  }

  public static <T> LazyComposableFuture<T> fromValue(final T value) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        consumer.consume(Try.fromValue(value));
      }
    });
  }

  public static <T> LazyComposableFuture<T> fromError(final Throwable error) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        consumer.consume(Try.<T>fromError(error));
      }
    });
  }

  public static <T> ComposableFuture<T> build(final Producer<T> producer) {
    return new LazyComposableFuture<>(producer);
  }

  public static <T> LazyComposableFuture<T> apply(final Supplier<T> supplier) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        try {
          consumer.consume(Try.fromValue(supplier.get()));
        } catch (final Exception e) {
          consumer.consume(Try.<T>fromError(e));
        }
      }
    });
  }

  public static <T> LazyComposableFuture<T> submit(final Executor executor, final Callable<T> task, final boolean delegateHandler) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        executor.execute(new Runnable() {
          @Override
          public void run() {
            try {
              consumer.consume(new Try.Success<>(task.call()));
            } catch (final Exception e) {
              consumer.consume(Try.<T>fromError(e));
            }
          }
        });
      }
    }, delegateHandler ? executor : null);
  }

  public static <T> LazyComposableFuture<T> schedule(final Scheduler scheduler, final Callable<T> task, final long delay, final TimeUnit timeUnit) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        scheduler.schedule(new Runnable() {
          @Override
          public void run() {
            try {
              consumer.consume(new Try.Success<>(task.call()));
            } catch (final Exception e) {
              consumer.consume(Try.<T>fromError(e));
            }
          }
        }, delay, timeUnit);
      }
    });
  }

  public static <T> LazyComposableFuture<T> collectFirst(final List<ComposableFuture<T>> futures) {
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        final AtomicBoolean done = new AtomicBoolean();
        for (final ComposableFuture<T> future : futures) {
          future.consume(new Consumer<T>() {
            @Override
            public void consume(final Try<T> result) {
              if (done.compareAndSet(false, true)) {
                consumer.consume(result);
              }
            }
          });
        }
      }
    });
  }

  public static <T> LazyComposableFuture<List<T>> collectAll(final List<ComposableFuture<T>> futures) {
    return new LazyComposableFuture<>(new Producer<List<T>>() {
      @Override
      public void produce(final Consumer<List<T>> consumer) {
        final AtomicInteger counter = new AtomicInteger(futures.size());
        final AtomicBoolean errorTrigger = new AtomicBoolean(false);
        final ConcurrentMap<Integer, Try<T>> results = new ConcurrentHashMap<>(futures.size());

        int index = 0;
        for (final ComposableFuture<T> future : futures) {
          final int i = index++;
          future.consume(new Consumer<T>() {
            @Override
            public void consume(final Try<T> result) {
              results.put(i, result);
              if (result.isSuccess()) {
                final int count = counter.decrementAndGet();
                if (count == 0) {
                  consumer.consume(Try.fromValue(createResultList(results)));
                }
              } else {
                if (errorTrigger.compareAndSet(false, true)) {
                  counter.set(0);
                  consumer.consume(Try.<List<T>>fromError(result.getError()));
                }
              }
            }
          });
        }
      }
    });
  }

  private static <T> List<T> createResultList(final ConcurrentMap<Integer, Try<T>> results) {
    final List<T> list = new ArrayList<>(results.size());
    for (int i = 0; i < results.size(); i++) {
      final Try<T> tryValue = results.get(i);
      list.add(tryValue != null ? tryValue.getValue() : null);
    }

    return list;
  }



  @Override
  public void consume(final Consumer<T> consumer) {
    if (executor != null) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          producer.produce(consumer);
        }
      });
    } else {
      producer.produce(consumer);
    }
  }

  public void consumeSync(final Consumer<T> consumer) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        consumer.consume(result);
        latch.countDown();
      }
    });

    latch.await();
  }

  @Override
  public <U> ComposableFuture<U> continueOnSuccess(final SuccessHandler<? super T, ? extends U> handler) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<U>() {
      @Override
      public void produce(final Consumer<U> consumer) {
        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> result) {
            if (result.isSuccess()) {
              try {
                consumer.consume(Try.fromValue(handler.handle(result.getValue())));
              } catch (final ExecutionException e) {
                final Throwable error = e.getCause() != null ? e.getCause() : e;
                consumer.consume(Try.<U>fromError(error));
              } catch (final Exception e) {
                consumer.consume(Try.<U>fromError(e));
              }
            } else {
              consumer.consume(Try.<U>fromError(result.getError()));
            }
          }
        });
      }
    });
  }

  @Override
  public <U> ComposableFuture<U> continueOnSuccess(final FutureSuccessHandler<? super T, U> handler) {
      final LazyComposableFuture<T> outer = this;
      return new LazyComposableFuture<>(new Producer<U>() {
        @Override
        public void produce(final Consumer<U> consumer) {
          outer.consume(new Consumer<T>() {
            @Override
            public void consume(final Try<T> result) {
              if (result.isSuccess()) {
                try {
                  final ComposableFuture<U> next = handler.handle(result.getValue());
                  if (next == null) {
                    consumer.consume(Try.<U>fromValue(null));
                  } else {
                    next.consume(new Consumer<U>() {
                      @Override
                      public void consume(final Try<U> nextResult) {
                        consumer.consume(nextResult);
                      }
                    });
                  }
                } catch (final Exception e) {
                  consumer.consume(Try.<U>fromError(e));
                }
              } else {
                consumer.consume(Try.<U>fromError(result.getError()));
              }
            }
          });
        }
      });
  }


  @Override
  public ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> result) {
            if (result.isSuccess()) {
              consumer.consume(result);
            } else {
              try {
                consumer.consume(Try.fromValue(handler.handle(result.getError())));
              } catch (final ExecutionException e) {
                consumer.consume(Try.<T>fromError(e));
              }
            }
          }
        });
      }
    });
  }

  @Override
  public ComposableFuture<T> continueOnError(final FutureErrorHandler<T> handler) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> result) {
            if (result.isSuccess()) {
              consumer.consume(result);
            } else {
              try {
                final ComposableFuture<T> next = handler.handle(result.getError());
                if (next == null) {
                  consumer.consume(Try.<T>fromValue(null));
                } else {
                  next.consume(new Consumer<T>() {
                    @Override
                    public void consume(final Try<T> nextResult) {
                      consumer.consume(nextResult);
                    }
                  });
                }
              } catch (final Exception e) {
                consumer.consume(Try.<T>fromError(e));
              }
            }
          }
        });
      }
    });
  }

  @Override
  public LazyComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit) {
    final LazyComposableFuture<T> deadline = new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        scheduler.schedule(new Runnable() {
          @Override
          public void run() {
            consumer.consume(Try.<T>fromError(new TimeoutException("timeout ended with no result.")));
          }
        }, timeout, unit);
      }
    });

    return collectFirst(Arrays.<ComposableFuture<T>>asList(this, deadline));
  }

  @Override
  public LazyComposableFuture<T> withTimeout(final long timeout, final TimeUnit unit) {
    return withTimeout(ComposableFutures.getScheduler(), timeout, unit);
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<R>() {
      @Override
      public void produce(final Consumer<R> consumer) {
        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> result) {
            try {
              consumer.consume(Try.fromValue(handler.handle(result)));
            } catch (final ExecutionException e) {
              final Throwable error = e.getCause() != null ? e.getCause() : e;
              consumer.consume(Try.<R>fromError(error));
            } catch (final Exception e) {
              consumer.consume(Try.<R>fromError(e));
            }
          }
        });
      }
    });
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<R>() {
      @Override
      public void produce(final Consumer<R> consumer) {
        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> result) {
            try {
              final ComposableFuture<R> next = handler.handle(result);
              if (next == null) {
                consumer.consume(Try.<R>fromValue(null));
              } else {
                next.consume(new Consumer<R>() {
                  @Override
                  public void consume(final Try<R> nextResult) {
                    consumer.consume(nextResult);
                  }
                });
              }

            } catch (final Exception e) {
              consumer.consume(Try.<R>fromError(e));
            }
          }
        });
      }
    });
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
  public ComposableFuture<T> materialize() {
    return ComposableFutures.buildEager(producer);
  }

  public LazyComposableFuture<T> doubleDispatch(final Scheduler scheduler, final long timeout, final TimeUnit unit) {
    final LazyComposableFuture<T> outer = this;
    return new LazyComposableFuture<>(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        final AtomicBoolean done = new AtomicBoolean();

        outer.consume(new Consumer<T>() {
          @Override
          public void consume(final Try<T> firstRes) {
            if (done.compareAndSet(false, true)) {
              consumer.consume(firstRes);
            }
          }
        });

        scheduler.schedule(new Runnable() {
          @Override
          public void run() {
            if (!done.get()) {
              outer.consume(new Consumer<T>() {
                @Override
                public void consume(final Try<T> secondRes) {
                  if (done.compareAndSet(false, true)) {
                    consumer.consume(secondRes);
                  }
                }
              });
            }
          }
        }, timeout, unit);
      }
    });
  }

  @Override
  public T get() throws ExecutionException, InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Try<T>> box = new AtomicReference<>();
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        box.set(result);
        latch.countDown();
      }
    });

    latch.await();
    final Try<T> res = box.get();
    if (res == null) {
      throw new ExecutionException(new NullPointerException("no result error."));
    } else if (res.isSuccess()) {
      return res.getValue();
    } else {
      throw new ExecutionException(res.getError());
    }
  }

  @Override
  public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    final CountDownLatch latch = new CountDownLatch(1);
    final AtomicReference<Try<T>> box = new AtomicReference<>();
    this.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        box.set(result);
        latch.countDown();
      }
    });

    latch.await(timeout, unit);
    final Try<T> res = box.get();
    if (res == null) {
      throw new ExecutionException(new NullPointerException("no result error."));
    } else if (res.isSuccess()) {
      return res.getValue();
    } else {
      throw new ExecutionException(res.getError());
    }
  }
}
