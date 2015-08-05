package com.outbrain.ob1k.cache;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.outbrain.ob1k.concurrent.ComposableFutures.newPromise;

/**
 * a wrapper for TypedCache implementation that delegate missing entries to a loader.
 * the loader is used a such a way that prevents concurrent activations on the same key.
 * <p/>
 * Created by aronen on 10/26/14.
 */
public class LoadingCacheDelegate<K, V> implements TypedCache<K, V> {
    private static final long DEFAULT_DURATION_MS = 500;

    private final TypedCache<K, V> cache;
    private final CacheLoader<K, V> loader;
    private final String cacheName;
    private final ConcurrentMap<K, ComposablePromise<V>> futureValues;

    private final long duration;
    private final TimeUnit timeUnit;

    private final Counter cacheHits;
    private final Counter cacheMiss;
    private final Counter cacheErrors;
    private final Counter loaderErrors;
    private final Counter cacheTimeouts;
    private final Counter loaderTimeouts;

    public LoadingCacheDelegate(final TypedCache<K, V> cache, final CacheLoader<K, V> loader, final String cacheName) {
        this(cache, loader, cacheName, null);
    }

    public LoadingCacheDelegate(final TypedCache<K, V> cache, final CacheLoader<K, V> loader, final String cacheName,
                                final MetricFactory metricFactory) {
        this(cache, loader, cacheName, metricFactory, DEFAULT_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    public LoadingCacheDelegate(final TypedCache<K, V> cache, final CacheLoader<K, V> loader, final String cacheName,
                                final MetricFactory metricFactory, final long duration, final TimeUnit timeUnit) {
        this.cache = cache;
        this.loader = loader;
        this.cacheName = cacheName;
        this.futureValues = new ConcurrentHashMap<>();

        this.duration = duration;
        this.timeUnit = timeUnit;

        if (metricFactory != null) {
            metricFactory.registerGauge("LoadingCacheDelegate." + cacheName, "mapSize", new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    return futureValues.size();
                }
            });

            cacheHits = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "hits");
            cacheMiss = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "miss");
            cacheErrors = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "cacheErrors");
            loaderErrors = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "loaderErrors");
            cacheTimeouts = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "cacheTimeouts");
            loaderTimeouts = metricFactory.createCounter("LoadingCacheDelegate." + cacheName, "loaderTimeouts");

        } else {
            cacheHits = null;
            cacheMiss = null;
            cacheErrors = null;
            loaderErrors = null;
            cacheTimeouts = null;
            loaderTimeouts = null;
        }
    }

    @Override
    public ComposableFuture<V> getAsync(final K key) {
        return ComposableFutures.build(new Producer<V>() {
            @Override
            public void produce(final Consumer<V> consumer) {
                final ComposablePromise<V> promise = newPromise();
                final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
                if (prev != null) {
                    consumeFrom(prev.future(), consumer);
                    return;
                }

                final ComposableFuture<V> cachedResult = cache.getAsync(key).withTimeout(duration, timeUnit);
                cachedResult.consume(new Consumer<V>() {
                    @Override
                    public void consume(final Try<V> res) {
                        if (res.isSuccess()) {
                            final V result = res.getValue();
                            if (result == null) {
                                if (cacheMiss != null) {
                                    cacheMiss.inc();
                                }

                                fetchFromLoader(key, promise);
                            } else {
                                if (cacheHits != null) {
                                    cacheHits.inc();
                                }
                                promise.set(result);
                                futureValues.remove(key);
                            }
                        } else {
                            final Throwable error = res.getError();
                            if (cacheErrors != null) {
                                cacheErrors.inc();
                                if(error instanceof TimeoutException) {
                                    cacheTimeouts.inc();
                                }
                            }
                            promise.setException(error);
                            futureValues.remove(key);
                        }
                    }
                });

                consumeFrom(promise.future(), consumer);
            }
        });
    }

    private void fetchFromLoader(final K key, final ComposablePromise<V> promise) {
        try {
            final ComposableFuture<V> loadedResult = loader.load(cacheName, key).withTimeout(duration, timeUnit);

            loadedResult.consume(new Consumer<V>() {
                @Override
                public void consume(final Try<V> loadedRes) {
                    if (loadedRes.isSuccess()) {
                        promise.set(loadedRes.getValue());
                        cache.setAsync(key, loadedRes.getValue()).consume(new Consumer<Boolean>() {
                            @Override
                            public void consume(final Try<Boolean> result) {
                                futureValues.remove(key);
                            }
                        });
                    } else {
                        final Throwable error = loadedRes.getError();
                        if (loaderErrors != null) {
                            loaderErrors.inc();
                            if(error instanceof TimeoutException) {
                                loaderTimeouts.inc();
                            }
                        }
                        promise.setException(error);
                        futureValues.remove(key);
                    }
                }
            });
        } catch (final Exception e) {
            // defensive coding, loader should not throw exceptions.
            promise.setException(e);
            futureValues.remove(key);
        }
    }

    private static <T> void consumeFrom(final ComposableFuture<T> source, final Consumer<T> consumer) {
        source.consume(new Consumer<T>() {
            @Override
            public void consume(final Try<T> result) {
                consumer.consume(result);
            }
        });
    }

    @Override
    public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
        return ComposableFutures.build(new Producer<Map<K, V>>() {
            @Override
            public void produce(final Consumer<Map<K, V>> consumer) {
                final List<? extends K> listKeys = Lists.newArrayList(keys);
                final List<K> processedKeys = new ArrayList<>();
                final Map<K, ComposablePromise<V>> res = new HashMap<>();

                for (final K key : listKeys) {
                    final ComposablePromise<V> promise = newPromise();
                    final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
                    if (prev == null) {
                        res.put(key, promise);
                        processedKeys.add(key);
                    } else {
                        res.put(key, prev);
                    }
                }

                final ComposableFuture<Map<K, V>> cachedResults = cache.getBulkAsync(processedKeys).withTimeout(duration, timeUnit);
                cachedResults.consume(new Consumer<Map<K, V>>() {
                    @Override
                    public void consume(final Try<Map<K, V>> res) {
                        if (res.isSuccess()) {
                            final Map<K, V> result = res.getValue();
                            final List<K> missingFromCacheKeys = new ArrayList<>();
                            for (final K key : processedKeys) {
                                if (result.containsKey(key)) {
                                    final ComposablePromise<V> promise = futureValues.get(key);
                                    promise.set(result.get(key));
                                    futureValues.remove(key);

                                    if (cacheHits != null) {
                                        cacheHits.inc();
                                    }
                                } else {
                                    missingFromCacheKeys.add(key);
                                    if (cacheMiss != null) {
                                        cacheMiss.inc();
                                    }
                                }
                            }

                            if (!missingFromCacheKeys.isEmpty()) {
                                fetchFromLoader(missingFromCacheKeys);
                            }
                        } else {
                            for (final K key : processedKeys) {
                                final ComposablePromise<V> promise = futureValues.get(key);
                                promise.setException(res.getError());
                                futureValues.remove(key);
                            }

                            if (cacheErrors != null) {
                                cacheErrors.inc();
                            }
                        }
                    }
                });

                consumeFrom(ComposableFutures.all(false, mapToFutures(res)), consumer);
            }
        });
    }

    private void fetchFromLoader(final List<K> missingFromCacheKeys) {
        try {
            final ComposableFuture<Map<K, V>> loadedResults = loader.load(cacheName, missingFromCacheKeys).withTimeout(duration, timeUnit);
            loadedResults.consume(new Consumer<Map<K, V>>() {
                @Override
                public void consume(final Try<Map<K, V>> loadedRes) {
                    if (loadedRes.isSuccess()) {
                        final Map<K, V> elements = loadedRes.getValue();
                        for (final K key : missingFromCacheKeys) {
                            futureValues.get(key).set(elements.get(key));
                        }

                        cache.setBulkAsync(elements).consume(new Consumer<Map<K, Boolean>>() {
                            @Override
                            public void consume(final Try<Map<K, Boolean>> setResults) {
                                for (final K key : missingFromCacheKeys) {
                                    futureValues.remove(key);
                                }
                            }
                        });

                    } else {
                        for (final K key : missingFromCacheKeys) {
                            final ComposablePromise<V> promise = futureValues.get(key);
                            promise.setException(loadedRes.getError());
                            futureValues.remove(key);
                        }

                        if (loaderErrors != null) {
                            loaderErrors.inc();
                        }
                    }
                }
            });
        } catch (final Exception e) {
            for (final K key : missingFromCacheKeys) {
                final ComposablePromise<V> promise = futureValues.get(key);
                promise.setException(e);
                futureValues.remove(key);
            }
        }
    }

    private static <K, V> Map<K, ComposableFuture<V>> mapToFutures(final Map<K, ComposablePromise<V>> promises) {
        final HashMap<K, ComposableFuture<V>> result = new HashMap<>(promises.size());
        for (final Map.Entry<K, ComposablePromise<V>> promiseEntry : promises.entrySet()) {
            result.put(promiseEntry.getKey(), promiseEntry.getValue().future());
        }

        return result;
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final K key, final V value) {
        return cache.setAsync(key, value);
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
        return cache.setAsync(key, mapper, maxIterations);
    }

    @Override
    public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
        return cache.setBulkAsync(entries);
    }

    @Override
    public ComposableFuture<Boolean> deleteAsync(final K key) {
        return cache.deleteAsync(key);
    }
}
