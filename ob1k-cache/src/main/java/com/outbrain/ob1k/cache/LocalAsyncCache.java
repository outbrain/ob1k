package com.outbrain.ob1k.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.all;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromNull;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * In-memory implementation for TypedCache
 *
 * @param <K> cache key type
 * @param <V> cache value type
 * @author aronen, marenzon, eran
 */
public class LocalAsyncCache<K, V> implements TypedCache<K, V> {

  private final GuavaTypedCache<K, V> guavaCache;
  private final TypedCache<K, V> typedCache;
  private final boolean failOnMissingEntries;

  // for testing purposes only!
  public LocalAsyncCache() {
    this(1000, 20, TimeUnit.SECONDS);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit) {
    this(maximumSize, ttl, unit, null);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader) {
    this(maximumSize, ttl, unit, loader, null, null);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName) {
    this(maximumSize, ttl, unit, loader, metricFactory, cacheName, false);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final MetricFactory metricFactory,
                         final String cacheName) {
    this(maximumSize, ttl, unit, null, metricFactory, cacheName, false);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName, final boolean failOnMissingEntries) {
    this.failOnMissingEntries = failOnMissingEntries;

    final boolean collectStats = metricFactory != null;
    final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder().
      maximumSize(maximumSize).
      expireAfterWrite(ttl, unit);

    final Cache<K, ComposableFuture<V>> cache = builder.build();

    if (collectStats) {
      builder.recordStats();
      GuavaCacheGaugesFactory.createGauges(metricFactory, cache, "LocalAsyncCache-" + cacheName);
    }

    this.guavaCache = new GuavaTypedCache<>(cache);
    this.typedCache = ofNullable(loader).
      map(kvCacheLoader ->
        (TypedCache<K, V>) new LoadingCacheDelegate<>(guavaCache, loader, cacheName, metricFactory, 1, HOURS)).
      orElse(guavaCache);
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    final ComposableFuture<V> cacheValue = typedCache.getAsync(key);
    return cacheValue.continueOnError((FutureErrorHandler<V>) error -> {
      guavaCache.asMap().remove(key, cacheValue);
      return fromError(error);
    });
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    return typedCache.getBulkAsync(keys);
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    return typedCache.deleteAsync(key);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return typedCache.setAsync(key, value);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return typedCache.setAsync(key, mapper, maxIterations);
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    return typedCache.setBulkAsync(entries);
  }

  private static class GuavaTypedCache<K, V> implements TypedCache<K, V> {

    private final Cache<K, ComposableFuture<V>> cache;

    private GuavaTypedCache(final Cache<K, ComposableFuture<V>> cache) {
      this.cache = requireNonNull(cache, "Guava's cache may not be null");
    }

    public ConcurrentMap<K, ComposableFuture<V>> asMap() {
      return cache.asMap();
    }

    @Override
    public ComposableFuture<V> getAsync(final K key) {
      return ofNullable(cache.getIfPresent(key)).orElse(fromNull());
    }

    @Override
    public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
      return all(false, cache.getAllPresent(keys));
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final K key, final V value) {
      cache.put(key, fromValue(value));
      return fromValue(true);
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
      final ConcurrentMap<K, ComposableFuture<V>> map = cache.asMap();

      try {
        if (maxIterations == 0) {
          return fromValue(false);
        }

        final ComposableFuture<V> currentFuture = map.get(key);
        if (currentFuture != null) {
          return currentFuture.continueOnSuccess((FutureSuccessHandler<V, Boolean>) currentValue -> {
            try {
              final V newValue = mapper.map(key, currentValue);
              if (newValue == null) {
                return fromValue(false);
              }

              final boolean success = map.replace(key, currentFuture, fromValue(newValue));
              if (success) {
                return fromValue(true);
              } else {
                return setAsync(key, mapper, maxIterations - 1);
              }
            } catch (final Exception e) {
              // in case mapper throws exception.
              return fromError(e);
            }
          });
        } else {
          final V newValue = mapper.map(key, null);
          if (newValue != null) {
            final ComposableFuture<V> prev = map.putIfAbsent(key, fromValue(newValue));
            if (prev == null) {
              return fromValue(true);
            } else {
              return setAsync(key, mapper, maxIterations - 1);
            }
          } else {
            return fromValue(false);
          }
        }
      } catch (final Exception e) {
        return fromValue(false);
      }
    }

    @Override
    public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
      final Map<K, ComposableFuture<Boolean>> result = new HashMap<>();
      for (final K key : entries.keySet()) {
        result.put(key, setAsync(key, entries.get(key)));
      }

      return all(false, result);
    }

    @Override
    public ComposableFuture<Boolean> deleteAsync(final K key) {
      cache.invalidate(key);
      return fromValue(true);
    }
  }
}
