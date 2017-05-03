package com.outbrain.ob1k.concurrent.lazy;

import com.google.common.base.Supplier;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Producer;
import com.outbrain.ob1k.concurrent.Scheduler;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.UncheckedExecutionException;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.ResultHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static java.util.function.Function.identity;

/**
 * a future that contains a producer. each time the future is consumed the producer is activated
 * to deliver a value or an error to the consumer.
 * <p/>
 * the final result is never stored so the future is actually stateless as opposed to the eager future
 * that eventually holds the final result.
 * <p/>
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
        return new LazyComposableFuture<>(consumer -> consumer.consume(Try.fromValue(value)));
    }

    public static <T> LazyComposableFuture<T> fromError(final Throwable error) {
        return new LazyComposableFuture<>(consumer -> consumer.consume(Try.fromError(error)));
    }

    public static <T> ComposableFuture<T> build(final Producer<T> producer) {
        return new LazyComposableFuture<>(producer);
    }

    public static <T> LazyComposableFuture<T> apply(final Supplier<T> supplier) {
        return new LazyComposableFuture<>(consumer ->
                consumer.consume(Try.apply(supplier::get)));
    }

    public static <T> LazyComposableFuture<T> submit(final Executor executor, final Callable<T> task, final boolean delegateHandler) {
        return new LazyComposableFuture<>(consumer -> executor.execute(() ->
                consumer.consume(Try.apply(task::call))), delegateHandler ? executor : null);
    }

    public static <T> LazyComposableFuture<T> schedule(final Scheduler scheduler, final Callable<T> task, final long delay, final TimeUnit timeUnit) {
        return new LazyComposableFuture<>(consumer -> scheduler.schedule(() ->
                consumer.consume(Try.apply(task::call)), delay, timeUnit));
    }

    public static <T> LazyComposableFuture<T> collectFirst(final List<ComposableFuture<T>> futures) {
        return new LazyComposableFuture<>(consumer -> {
            final AtomicBoolean done = new AtomicBoolean();
            for (final ComposableFuture<T> future : futures) {
                future.consume(result -> {
                    if (done.compareAndSet(false, true)) {
                        consumer.consume(result);
                    }
                });
            }
        });
    }

    public static <T> LazyComposableFuture<List<T>> collectAll(final List<ComposableFuture<T>> futures) {
        return new LazyComposableFuture<>(consumer -> {
            final AtomicInteger counter = new AtomicInteger(futures.size());
            final AtomicBoolean errorTrigger = new AtomicBoolean(false);
            final ConcurrentMap<Integer, Try<T>> results = new ConcurrentHashMap<>(futures.size());

            int index = 0;
            for (final ComposableFuture<T> future : futures) {
                final int i = index++;
                future.consume(result -> {
                    results.put(i, result);
                    if (result.isSuccess()) {
                        final int count = counter.decrementAndGet();
                        if (count == 0) {
                            consumer.consume(Try.fromValue(createResultList(results)));
                        }
                    } else {
                        if (errorTrigger.compareAndSet(false, true)) {
                            counter.set(0);
                            consumer.consume(Try.fromError(result.getError()));
                        }
                    }
                });
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
    public <R> ComposableFuture<R> map(final Function<? super T, ? extends R> mapper) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            if (result.isSuccess()) {
                try {
                    consumer.consume(Try.fromValue(mapper.apply(result.getValue())));
                } catch (final UncheckedExecutionException e) {
                    final Throwable error = e.getCause() != null ? e.getCause() : e;
                    consumer.consume(Try.fromError(error));
                } catch (final Throwable e) {
                    consumer.consume(Try.fromError(e));
                }
            } else {
                consumer.consume(Try.fromError(result.getError()));
            }
        }));
    }

    @Override
    public <R> ComposableFuture<R> flatMap(final Function<? super T, ? extends ComposableFuture<? extends R>> mapper) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            if (result.isSuccess()) {
                try {
                    final ComposableFuture<? extends R> next = mapper.apply(result.getValue());
                    if (next == null) {
                        consumer.consume(Try.fromValue(null));
                    } else {
                        next.consume(consumer);
                    }
                } catch (final Throwable e) {
                    consumer.consume(Try.fromError(e));
                }
            } else {
                consumer.consume(Try.fromError(result.getError()));
            }
        }));
    }

    @Override
    public <E extends Throwable> ComposableFuture<T> recover(Class<E> errorType, Function<E, ? extends T> recover) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            if (result.isSuccess()) {
                consumer.consume(result);
            } else {
                try {
                    Throwable error = result.getError();
                    if (errorType.isInstance(error)) {
                        E matchedError = errorType.cast(error);
                        consumer.consume(Try.fromValue(recover.apply(matchedError)));
                    } else {
                        consumer.consume(result);
                    }
                } catch (final UncheckedExecutionException e) {
                    consumer.consume(Try.fromError(e.getCause() != null ? e.getCause() : e));
                } catch (final Throwable e) {
                    consumer.consume(Try.fromError(e));
                }
            }
        }));
    }

    @Override
    public <E extends Throwable> ComposableFuture<T>
        recoverWith(Class<E> errorType, final Function<E, ? extends ComposableFuture<? extends T>> recover) {

        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            if (result.isSuccess()) {
                consumer.consume(result);
            } else {
                try {
                    Throwable error = result.getError();
                    if (errorType.isInstance(error)) {
                        E matchedError = errorType.cast(error);
                        final ComposableFuture<? extends T> next = recover.apply(matchedError);
                        if (next == null) {
                            consumer.consume(Try.fromValue(null));
                        } else {
                            next.consume(consumer);
                        }
                    } else {
                        consumer.consume(result);
                    }
                } catch (final Throwable e) {
                    consumer.consume(Try.fromError(e));
                }
            }
        }));
    }

    @Override
    public <R> ComposableFuture<R> always(final Function<Try<T>, ? extends R> handler) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            try {
                consumer.consume(Try.fromValue(handler.apply(result)));
            } catch (final UncheckedExecutionException e) {
                final Throwable error = e.getCause() != null ? e.getCause() : e;
                consumer.consume(Try.fromError(error));
            } catch (final Throwable e) {
                consumer.consume(Try.fromError(e));
            }
        }));
    }

    @Override
    public <R> ComposableFuture<R> alwaysWith(final Function<Try<T>, ? extends ComposableFuture<? extends R>> handler) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            try {
                final ComposableFuture<? extends R> next = handler.apply(result);
                if (next == null) {
                    consumer.consume(Try.fromValue(null));
                } else {
                    next.consume(consumer);
                }
            } catch (final Throwable e) {
                consumer.consume(Try.fromError(e));
            }
        }));
    }

    @Override
    public ComposableFuture<T> andThen(final Consumer<? super T> resultConsumer) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> outer.consume(result -> {
            resultConsumer.consume(result.map(identity()));
            consumer.consume(result);
        }));
    }

    @Override
    public void consume(final Consumer<? super T> consumer) {
        if (executor != null) {
            executor.execute(() -> consumeValue(consumer));
        } else {
            consumeValue(consumer);
        }
    }

    private void consumeValue(final Consumer<? super T> consumer) {
        producer.produce(valueTry -> consumer.consume(valueTry.map(identity())));
    }

    @Override
    public LazyComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit,
                                               final String taskDescription) {
        final LazyComposableFuture<T> deadline = new LazyComposableFuture<>(consumer -> scheduler.schedule(() ->
                        consumer.consume(Try.fromError(
                                new TimeoutException("Timeout occurred on task ('" +
                                        taskDescription + "' " +
                                        timeout + " " +
                                        unit + ")"))),
                timeout, unit));

        return collectFirst(Arrays.asList(this, deadline));
    }

    @Override
    public LazyComposableFuture<T> withTimeout(final Scheduler scheduler, final long timeout, final TimeUnit unit) {
        return withTimeout(scheduler, timeout, unit, "unspecified context");
    }

    @Override
    public LazyComposableFuture<T> withTimeout(final long timeout, final TimeUnit unit, final String taskDescription) {
        return withTimeout(ComposableFutures.getScheduler(), timeout, unit, taskDescription);
    }

    @Override
    public LazyComposableFuture<T> withTimeout(final long timeout, final TimeUnit unit) {
        return withTimeout(ComposableFutures.getScheduler(), timeout, unit);
    }

    @Override
    public ComposableFuture<T> materialize() {
        return ComposableFutures.buildEager(producer);
    }

    public LazyComposableFuture<T> doubleDispatch(final Scheduler scheduler, final long timeout, final TimeUnit unit) {
        final LazyComposableFuture<T> outer = this;
        return new LazyComposableFuture<>(consumer -> {
            final AtomicBoolean done = new AtomicBoolean();

            outer.consume(firstRes -> {
                if (done.compareAndSet(false, true)) {
                    consumer.consume(firstRes);
                }
            });

            scheduler.schedule(() -> {
                if (!done.get()) {
                    outer.consume(secondRes -> {
                        if (done.compareAndSet(false, true)) {
                            consumer.consume(secondRes);
                        }
                    });
                }
            }, timeout, unit);
        });
    }


  /*
    OLD API - DEPRECATED
 */


    @Override
    @SuppressWarnings({"unchecked", "deprecated"})
    public <U> ComposableFuture<U> continueOnSuccess(final SuccessHandler<? super T, ? extends U> handler) {
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
    @SuppressWarnings({"unchecked", "deprecated"})
    public <U> ComposableFuture<U> continueOnSuccess(final FutureSuccessHandler<? super T, U> handler) {
        return flatMap(handler::handle);
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecated"})
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
    public ComposableFuture<T> continueOnError(final FutureErrorHandler<T> handler) {
        return recoverWith(handler::handle);
    }

    @Override
    public <R> ComposableFuture<R> continueWith(final ResultHandler<T, R> handler) {
        return always(result -> {
            try {
                return handler.handle(result);
            } catch (final ExecutionException e) {
                // new API doesn't allows checked exceptions - offering throwing runtime instead. sadly, doing this for BC.
                throw new UncheckedExecutionException(e);
            }
        });
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecated"})
    public <R> ComposableFuture<R> continueWith(final FutureResultHandler<T, R> handler) {
        return alwaysWith(handler::handle);
    }

    @Override
    public <R> ComposableFuture<R> transform(final com.google.common.base.Function<? super T, ? extends R> function) {
        return map(function::apply);
    }
}
