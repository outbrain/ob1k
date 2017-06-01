package com.outbrain.ob1k.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ExecutionError;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.UncheckedExecutionException;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.outbrain.ob1k.concurrent.ComposableFutures.all;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromNull;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;


/**
 * User: aronen
 * Date: 6/30/13
 * Time: 6:08 PM
 */
public class LocalAsyncCache<K,V> implements TypedCache<K,V> {
  private final LoadingCache<K, ComposableFuture<V>> loadingCache;
  private final Cache<K, ComposableFuture<V>> localCache;
  private final CacheLoader<K, V> loader;
  private final String cacheName;
  private final boolean failOnMissingEntries;

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName) {
    this(maximumSize, ttl, unit, loader, metricFactory, cacheName, false);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName, final boolean failOnMissingEntries) {
    this.loader = loader;
    this.cacheName = cacheName;
    this.failOnMissingEntries = failOnMissingEntries;

    final boolean collectStats = metricFactory != null;
    final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
        .maximumSize(maximumSize)
        .expireAfterWrite(ttl, unit);

    if (collectStats)
      builder.recordStats();

    this.loadingCache = builder
        .build(new com.google.common.cache.CacheLoader<K, ComposableFuture<V>>() {
          public ComposableFuture<V> load(final K key) throws Exception {
            return loadElement(key);
          }

          @Override
          public Map<K, ComposableFuture<V>> loadAll(final Iterable<? extends K> keys) throws Exception {
            return loadElements(Lists.newArrayList(keys));
          }
        });

    if (collectStats) {
      GuavaCacheGaugesFactory.createGauges(metricFactory, loadingCache, "LocalAsyncCache-" + cacheName);
    }

    this.localCache = null;
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader) {
    this(maximumSize, ttl, unit, loader, null, null);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final MetricFactory metricFactory, final String cacheName) {
    this.loader = null;
    this.loadingCache = null;
    this.failOnMissingEntries = true; // fake value, not in use.
    this.cacheName = cacheName;

    final boolean collectStats = metricFactory != null;
    final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
        .maximumSize(maximumSize)
        .expireAfterWrite(ttl, unit);

    if (collectStats) {
      builder.recordStats();
    }

    this.localCache = builder.build();
    if (collectStats) {
      GuavaCacheGaugesFactory.createGauges(metricFactory, localCache, "LocalAsyncCache-" + cacheName);
    }
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit) {
    this(maximumSize, ttl, unit, null, null);
  }

  // for testing purposes only!
  public LocalAsyncCache() {
    this(1000, 20, TimeUnit.SECONDS);
  }

  private ComposableFuture<V> loadElement(final K key) {
    return loader.load(cacheName, key).materialize();
  }

  private Function<Map<K, V>, ComposableFuture<V>> extractLoaderResultEntry(final K key) {
    return loaderResults -> {
    final V res = loaderResults.get(key);
    if (res != null) {
      return fromValue(res);
    } else {
      if (failOnMissingEntries) {
        final String prefix = cacheName == null ? "" : cacheName + ": ";
        return fromError(new RuntimeException(prefix + "result for " + key + " is missing from loader response."));
      } else {
        return fromNull();
      }
    }
  };
  }

  private Map<K, ComposableFuture<V>> loadElements(final Iterable<? extends K> keys) {
    final ComposableFuture<Map<K, V>> loaded = loader.load(cacheName, keys).materialize();
    final Map<K, ComposableFuture<V>> result = new HashMap<>();
    for (final K key : keys) {
      result.put(key, loaded.flatMap(extractLoaderResultEntry(key)));
    }
    return result;
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    try {
      if (loadingCache != null) {
        final ComposableFuture<V> res = loadingCache.get(key);
        if (res == null) {
          return fromNull();
        }
        return res.recoverWith(error -> {
          loadingCache.asMap().remove(key, res);
          return fromError(error);
        });
      } else {
        final ComposableFuture<V> res = localCache.getIfPresent(key);
        if (res == null) {
          return fromNull();
        }
        return res;
      }
    } catch (final com.google.common.util.concurrent.UncheckedExecutionException e) {
      return fromError(e.getCause());
    } catch (final ExecutionException | UncheckedExecutionException | ExecutionError e) {
      return fromError(e.getCause());
    }
  }

  private void unloadErrorsFromCache(final ImmutableMap<K, ComposableFuture<V>> innerMap, final Iterable<? extends K> keys) {
    for (final K key : keys) {
      innerMap.get(key).consume(result -> {
        if (!result.isSuccess() || result.getValue() == null) {
          loadingCache.asMap().remove(key);
        }
      });
    }
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    try {
      final ImmutableMap<K, ComposableFuture<V>> innerMap;
      if (loadingCache != null) {
        innerMap = loadingCache.getAll(keys);
        unloadErrorsFromCache(innerMap, keys);
      } else {
        innerMap = localCache.getAllPresent(keys);
      }

      final Map<K, ComposableFuture<V>> result = new HashMap<>();
      for (final K key : innerMap.keySet()) {
        final ComposableFuture<V> value = innerMap.get(key);
        result.put(key, value);
      }

      return all(true, result);
    } catch (final com.google.common.util.concurrent.UncheckedExecutionException e) {
      return fromError(e.getCause());
    } catch (final ExecutionException | UncheckedExecutionException | ExecutionError e) {
      return fromError(e.getCause());
    }
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    if (loadingCache != null) {
      loadingCache.invalidate(key);
    } else {
      localCache.invalidate(key);
    }

    return fromValue(true);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    if (loadingCache != null) {
      loadingCache.put(key, fromValue(value));
    } else {
      localCache.put(key, fromValue(value));
    }

    return fromValue(true);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    final ConcurrentMap<K, ComposableFuture<V>> map = loadingCache != null ? loadingCache.asMap() : localCache.asMap();
    try {
      if (maxIterations == 0) {
        return fromValue(false);
      }

      final ComposableFuture<V> currentFuture = map.get(key);
      if (currentFuture != null) {
        return currentFuture.flatMap(currentValue -> {
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

}
