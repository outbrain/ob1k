package com.outbrain.ob1k.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.*;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromNull;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;


/**
 * User: aronen
 * Date: 6/30/13
 * Time: 6:08 PM
 */
public class LocalAsyncCache<K,V> implements TypedCache<K,V> {
  private static final Logger logger = LoggerFactory.getLogger(LocalAsyncCache.class);

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
    final ComposableFuture<V> loadedElement = loader.load(cacheName, key).materialize();
    return loadedElement.continueOnError(new FutureErrorHandler<V>() {
      @Override
      public ComposableFuture<V> handle(final Throwable error) {
        loadingCache.asMap().remove(key, loadedElement);
        return fromError(error);
      }
    });
  }

  private Map<K, ComposableFuture<V>> loadElements(final Iterable<? extends K> keys) {
    final ComposableFuture<Map<K, V>> loaded = loader.load(cacheName, keys).materialize();
    final Map<K, ComposableFuture<V>> result = new HashMap<>();
    for (final K key : keys) {
      result.put(key, loaded.continueOnError(new FutureErrorHandler<Map<K, V>>() {
        @Override
        public ComposableFuture<Map<K, V>> handle(final Throwable error) {
          loadingCache.asMap().remove(key);
          return fromError(error);
        }
      }).continueOnSuccess(new FutureSuccessHandler<Map<K, V>, V>() {
        @Override
        public ComposableFuture<V> handle(final Map<K, V> result) {
          final V res = result.get(key);
          if (res != null) {
            return fromValue(res);
          } else {
            loadingCache.asMap().remove(key);
            if (failOnMissingEntries) {
              return fromError(new RuntimeException("result is missing from loader response."));
            } else {
              return fromNull();
            }
          }
        }
      }));
    }

    return result;
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    try {
      if (loadingCache != null) {
        ComposableFuture<V> res = loadingCache.get(key);
        if (res == null) {
          res = fromValue(null);
        }
        return res;
      } else {
        ComposableFuture<V> res = localCache.getIfPresent(key);
        if (res == null) {
          res = fromValue(null);
        }
        return res;
      }
    } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
      return ComposableFutures.fromError(e.getCause());
    }
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    try {
      final ImmutableMap<K, ComposableFuture<V>> innerMap;
      if (loadingCache != null) {
        innerMap = loadingCache.getAll(keys);
      } else {
        innerMap = localCache.getAllPresent(keys);
      }

      final Map<K, ComposableFuture<V>> result = new HashMap<>();
      for (final K key: innerMap.keySet()) {
        final ComposableFuture<V> value = innerMap.get(key);
        result.put(key, value);
      }

      return ComposableFutures.all(true, result);
    } catch (ExecutionException | UncheckedExecutionException | ExecutionError e) {
      return ComposableFutures.fromError(e.getCause());
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
      return currentFuture.continueOnSuccess(new FutureSuccessHandler<V, Boolean>() {
        @Override
        public ComposableFuture<Boolean> handle(final V currentValue) {
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
        }
      });

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

    return ComposableFutures.all(false, result);
  }

}
