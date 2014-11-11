package com.outbrain.ob1k.concurrent;

import com.google.common.base.Function;
import com.outbrain.ob1k.concurrent.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * User: aronen
 * Date: 6/6/13
 * Time: 2:08 PM
 */
public class SimpleComposableFuture<T> extends FutureTask<T> implements ComposablePromise<T> {
  private static final Logger logger = LoggerFactory.getLogger(SimpleComposableFuture.class);

  private final ComposableExecutorService threadPool;
  private final HandlersList handlers;
  private volatile Throwable error;

  protected SimpleComposableFuture() {
    super(new Callable<T>() {
      @Override public T call() throws Exception {
        return null;
      }
    });
    threadPool = null;
    handlers = new HandlersList();
  }

  protected SimpleComposableFuture(final ComposableExecutorService threadPool) {
    super(new Callable<T>() {
      @Override public T call() throws Exception {
        return null;
      }
    });
    this.threadPool = threadPool;
    handlers = new HandlersList();
  }

  protected SimpleComposableFuture(final Callable<T> task) {
    super(task);
    threadPool = null;
    handlers = new HandlersList();
  }


  protected SimpleComposableFuture(final Callable<T> task, final ComposableExecutorService threadPool) {
    super(task);
    this.threadPool = threadPool;
    handlers = new HandlersList();
  }

  protected SimpleComposableFuture(final Runnable task, final T value) {
    super(task, value);
    threadPool = null;
    handlers = new HandlersList();
  }

  @Override
  public void set(final T result) {
    error = null;
    super.set(result);
  }

  @Override
  public void setException(final Throwable t) {
    error = t;
    super.setException(t);
  }

  @Override
  protected void done() {
    handlers.execute(threadPool);
  }

  @Override
  public boolean isSuccess() {
    return error == null;
  }

  @Override
  public State getState() {
    if (isDone()) {
      return isSuccess() ? State.Success : State.Failure;
    } else {
      return State.Waiting;
    }
  }

  @Override
  public Throwable getError() {
    return error;
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
    final ComposablePromise<R> result = new SimpleComposableFuture<>(threadPool);
    this.onResult(new OnResultHandler<T>() {
      @Override
      public void handle(final ComposableFuture<T> res) {
        try {
          final ComposableFuture<R> nextResult = handler.handle(res);
          nextResult.onSuccess(new OnSuccessHandler<R>() {
            @Override
            public void handle(final R element) {
              result.set(element);
            }
          });

          nextResult.onError(new OnErrorHandler() {
            @Override
            public void handle(final Throwable error) {
              result.setException(error);
            }
          });

        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    return result;
  }

  @Override
  public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
    final ComposablePromise<R> result = new SimpleComposableFuture<>(threadPool);
    this.onResult(new OnResultHandler<T>() {
      @Override
      public void handle(final ComposableFuture<T> res) {
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
  public <R> ComposableFuture<R> continueOnSuccess(final FutureSuccessHandler<? super T, ? extends R> handler) {
    final ComposablePromise<R> result = new SimpleComposableFuture<>(threadPool);
    this.onSuccess(new OnSuccessHandler<T>() {
      @Override
      public void handle(final T element) {
        try {
          final ComposableFuture<? extends R> res = handler.handle(element);
          res.onSuccess(new OnSuccessHandler<R>() {
            @Override
            public void handle(final R element) {
              result.set(element);
            }
          });

          res.onError(new OnErrorHandler() {
            @Override
            public void handle(final Throwable error) {
              result.setException(error);
            }
          });

        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    this.onError(new OnErrorHandler() {
      @Override
      public void handle(final Throwable error) {
        result.setException(error);
      }
    });

    return result;
  }

  @Override
  public <R> ComposableFuture<R> continueOnSuccess(final SuccessHandler<? super T, ? extends R> handler) {
    final ComposablePromise<R> result = new SimpleComposableFuture<>(threadPool);
    this.onSuccess(new OnSuccessHandler<T>() {
      @Override
      public void handle(final T element) {
        try {
          result.set(handler.handle(element));
        } catch (final ExecutionException e) {
          result.setException(e.getCause() != null ? e.getCause() : e);
        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    this.onError(new OnErrorHandler() {
      @Override
      public void handle(final Throwable error) {
        result.setException(error);
      }
    });

    return result;
  }

  @Override
  public ComposableFuture<T> continueOnError(final FutureErrorHandler<? extends T> handler) {
    final ComposablePromise<T> result = new SimpleComposableFuture<>(threadPool);
    this.onSuccess(new OnSuccessHandler<T>() {
      @Override
      public void handle(final T element) {
        result.set(element);
      }
    });

    this.onError(new OnErrorHandler() {
      @Override
      public void handle(final Throwable error) {
        try {
          final ComposableFuture<? extends T> res = handler.handle(error);
          res.onSuccess(new OnSuccessHandler<T>() {
            @Override
            public void handle(final T element) {
              result.set(element);
            }
          });

          res.onError(new OnErrorHandler() {
            @Override
            public void handle(final Throwable error) {
              result.setException(error);
            }
          });
        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    return result;
  }

  @Override
  public ComposableFuture<T> continueOnError(final ErrorHandler<? extends T> handler) {
    final ComposablePromise<T> result = new SimpleComposableFuture<>(threadPool);
    this.onSuccess(new OnSuccessHandler<T>() {
      @Override
      public void handle(final T element) {
        result.set(element);
      }
    });

    this.onError(new OnErrorHandler() {
      @Override
      public void handle(final Throwable error) {
        try {
          result.set(handler.handle(error));
        } catch (final ExecutionException e) {
          result.setException(e.getCause() != null ? e.getCause() : e);
        } catch (final Exception e) {
          result.setException(e);
        }
      }
    });

    return result;
  }

  @Override
  public void onResult(final OnResultHandler<T> handler) {
    handlers.addHandler(new OnResultAction<>(handler, this), threadPool);
  }

  @Override
  public void onSuccess(final OnSuccessHandler<? super T> handler) {
    handlers.addHandler(new OnSuccessAction<>(handler, this), threadPool);
  }

  @Override
  public void onError(final OnErrorHandler handler) {
    handlers.addHandler(new OnErrorAction<>(handler, this), threadPool);
  }

  @Override
  public <R> ComposableFuture<R> transform(final Function<? super T, ? extends R> function) {
    return continueOnSuccess(new SuccessHandler<T, R>() {
      @Override
      public R handle(final T result) throws ExecutionException {
        return function.apply(result);
      }
    });
  }

  @Override
  public ComposableFuture<T> withTimeout(final long duration, final TimeUnit unit) {
    return ComposableFutures.withTimeout(this, duration, unit);
  }

  private static class OnSuccessAction<T> implements Runnable {
    final OnSuccessHandler<? super T> handler;
    final ComposableFuture<T> current;

    private OnSuccessAction(final OnSuccessHandler<? super T> handler, final ComposableFuture<T> current) {
      this.handler = handler;
      this.current = current;
    }

    @Override
    public void run() {
      try {
        if (current.getState() == State.Success) {
          handler.handle(current.get());
        }
      } catch (final Throwable error) {
        logger.warn("error while handling future callbacks", error);
      }
    }
  }

  private static class OnErrorAction<T> implements Runnable {
    final OnErrorHandler handler;
    final ComposableFuture<T> current;

    private OnErrorAction(final OnErrorHandler handler, final ComposableFuture<T> current) {
      this.handler = handler;
      this.current = current;
    }

    @Override
    public void run() {
      try {
        if (current.getState() == State.Failure) {
          handler.handle(current.getError());
        }
      } catch (final Throwable error) {
        logger.warn("error while handling future callbacks", error);
      }
    }
  }

  private static class OnResultAction<T> implements Runnable {
    final OnResultHandler<T> inner;
    final ComposableFuture<T> current;

    private OnResultAction(final OnResultHandler<T> inner, final ComposableFuture<T> current) {
      this.inner = inner;
      this.current = current;
    }

    @Override
    public void run() {
      try {
        inner.handle(current);
      } catch (final Throwable error) {
        logger.warn("error while handling future callbacks", error);
      }
    }
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || !(o instanceof ComposableFuture)) return false;

    final ComposableFuture that = (ComposableFuture) o;
    final State thatState = that.getState();
    final State thisState = getState();

    if (thisState != thatState) {
      return false;
    }

    if (thisState != State.Success && thisState != State.Failure) {
      return false;
    }

    if (thisState == State.Success) {
      try {
        final Object thisValue = this.get();
        final Object thatValue = that.get();
        return thisValue == null ? thatValue == null : thisValue.equals(thatValue);
      } catch (final Exception e) {
        return false;
      }
    } else /* must be failure state */ {
      return error.equals(that.getError());
    }
  }

  @Override
  public int hashCode() {
    final State state = getState();
    if (state == State.Waiting) {
      return super.hashCode();
    }

    if (state == State.Success) {
      try {
        final T thisValue = this.get();
        return thisValue != null ? thisValue.hashCode() : 0;
      } catch (final Exception e) {
        return super.hashCode(); // super weird...
      }
    }

    return error.hashCode();
  }

}
