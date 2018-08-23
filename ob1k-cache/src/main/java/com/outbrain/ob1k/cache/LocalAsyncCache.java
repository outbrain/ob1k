package com.outbrain.ob1k.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.UncheckedExecutionException;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
  private static final int DEFAULT_LOAD_TIMEOUT = 1000;
  private static final TimeUnit DEFAULT_LOAD_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;
  private static final int DEFAULT_REFRESH_THREAD_COUNT = 10;

  private final LoadingCache<K, ComposableFuture<V>> loadingCache;
  private final Cache<K, ComposableFuture<V>> localCache;

  private final ExecutorService executor;

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName) {
    this(maximumSize, ttl, unit, loader, metricFactory, cacheName, false);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName, final boolean failOnMissingEntries) {
    this(maximumSize, ttl, unit, loader,  metricFactory, cacheName, failOnMissingEntries, DEFAULT_LOAD_TIMEOUT, DEFAULT_LOAD_TIMEOUT_UNIT, -1, null, DEFAULT_REFRESH_THREAD_COUNT);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName, final boolean failOnMissingEntries,
                         final long loadTimeout, final TimeUnit loadTimeoutUnit,
                         final long refreshAfterWriteDuration, final TimeUnit refreshAfterWriteUnit, final int refreshThreadCount) {
    final boolean collectStats = metricFactory != null;
    final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(ttl, unit);

    if (refreshAfterWriteDuration != -1) {
      builder.refreshAfterWrite(refreshAfterWriteDuration, refreshAfterWriteUnit);

      if (refreshThreadCount < 1) {
        this.executor = Executors.newFixedThreadPool(DEFAULT_REFRESH_THREAD_COUNT);
      } else {
        this.executor = Executors.newFixedThreadPool(refreshThreadCount);
      }

    } else {
      this.executor = null;
    }

    if (collectStats)
      builder.recordStats();

    this.loadingCache = builder.build(new InternalCacheLoader(loader, cacheName, failOnMissingEntries, loadTimeout, loadTimeoutUnit));

    if (collectStats) {
      GuavaCacheGaugesFactory.createGauges(metricFactory, loadingCache, "LocalAsyncCache-" + cacheName);
    }

    this.localCache = null;
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader) {
    this(maximumSize, ttl, unit, loader, null, null);
  }

  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final MetricFactory metricFactory, final String cacheName) {
    this.loadingCache = null;
    this.executor = null;

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
    } catch (final com.google.common.util.concurrent.UncheckedExecutionException | ExecutionException | UncheckedExecutionException | ExecutionError e) {
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
    } catch (final com.google.common.util.concurrent.UncheckedExecutionException | ExecutionException | UncheckedExecutionException | ExecutionError e) {
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
  public ComposableFuture<Boolean> setIfAbsentAsync(final K key, final V value) {
    final Cache<K, ComposableFuture<V>> cache = loadingCache == null ? localCache : loadingCache;
    return fromValue(cache.asMap().putIfAbsent(key, fromValue(value)) == null);
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

  public static class Builder<K, V> {
    private final int maximumSize;
    private final int ttl;
    private final TimeUnit unit;
    private final MetricFactory metricFactory;
    private final String cacheName;
    private CacheLoader<K, V> loader;
    private boolean failOnMissingEntries = false;
    private long refreshAfterWriteDuration;
    private TimeUnit refreshAfterWriteUnit;
    private long loadTimeout = DEFAULT_LOAD_TIMEOUT;
    private TimeUnit loadTimeoutUnit = DEFAULT_LOAD_TIMEOUT_UNIT;
    private int refreshThreadCount = DEFAULT_REFRESH_THREAD_COUNT;

    public Builder(final int maximumSize, final int ttl, final TimeUnit unit, final MetricFactory metricFactory, final String cacheName) {
      this.maximumSize = maximumSize;
      this.ttl = ttl;
      this.unit = unit;
      this.metricFactory = metricFactory;
      this.cacheName = cacheName;
    }

    public Builder<K, V> withLoader(final CacheLoader<K, V> loader) {
      this.loader = loader;
      return this;
    }

    public Builder<K, V> withLoadTimeout(final long loadTimeout, final TimeUnit loadTimeoutUnit) {
      this.loadTimeout = loadTimeout;
      this.loadTimeoutUnit = loadTimeoutUnit;
      return this;
    }

    public Builder<K, V> failOnMissingEntries() {
      this.failOnMissingEntries = true;
      return this;
    }

    public Builder<K, V> refreshAfterWrite(long duration, TimeUnit unit) {
      this.refreshAfterWriteDuration = duration;
      this.refreshAfterWriteUnit = unit;
      return this;
    }

    public Builder<K, V> withRefreshThreadCount(int refreshThreadCount) {
      this.refreshThreadCount = refreshThreadCount;
      return this;
    }

    public LocalAsyncCache<K, V> build() {
      if (loader == null) {
        return new LocalAsyncCache<>(maximumSize, ttl, unit, metricFactory, cacheName);
      }
      return new LocalAsyncCache<>(maximumSize, ttl, unit, loader, metricFactory, cacheName, failOnMissingEntries,
              loadTimeout, loadTimeoutUnit, refreshAfterWriteDuration, refreshAfterWriteUnit, refreshThreadCount);
    }
  }

  private class InternalCacheLoader extends com.google.common.cache.CacheLoader<K, ComposableFuture<V>> {

    private final CacheLoader<K, V> loader;
    private final String cacheName;
    private final boolean failOnMissingEntries;
    private final long loadTimeout;
    private final TimeUnit loadTimeoutUnit;

    InternalCacheLoader(final CacheLoader<K, V> loader, final String cacheName, final boolean failOnMissingEntries,
                        final long loadTimeout, final TimeUnit loadTimeoutUnit) {
      this.loader = loader;
      this.cacheName = cacheName;
      this.failOnMissingEntries = failOnMissingEntries;
      this.loadTimeout = loadTimeout;
      this.loadTimeoutUnit = loadTimeoutUnit;
    }

    @Override
    public ComposableFuture<V> load(@Nonnull final K key) {
      return loader.load(cacheName, key).withTimeout(loadTimeout, loadTimeoutUnit).materialize();
    }

    @Override
    public Map<K, ComposableFuture<V>> loadAll(final Iterable<? extends K> keys) {
      final ComposableFuture<Map<K, V>> loaded = loader.load(cacheName, keys).withTimeout(loadTimeout, loadTimeoutUnit).materialize();
      final Map<K, ComposableFuture<V>> result = new HashMap<>();
      for (final K key : keys) {
        result.put(key, loaded.flatMap(extractLoaderResultEntry(key)));
      }
      return result;
    }

    @Override
    public ListenableFuture<ComposableFuture<V>> reload(final K key, final ComposableFuture<V> oldValue) {
      ListenableFutureTask<ComposableFuture<V>> task = ListenableFutureTask.create(() -> {
        ComposableFuture<V> loadFuture = loader.load(cacheName, key).recoverWith(e -> oldValue);
        V value = loadFuture.get(loadTimeout, loadTimeoutUnit);
        return ComposableFutures.fromValue(value);
      });

      executor.execute(task);
      return task;
    }

    private Function<Map<K, V>, ComposableFuture<V>> extractLoaderResultEntry(final K key) {
      return loaderResults -> {
        final V res = loaderResults.get(key);
        if (res != null) {
          return fromValue(res);
        } else {
          if (failOnMissingEntries) {
            return fromError(new RuntimeException(key + " is missing from" + (cacheName == null ? "" : " " + cacheName) + " loader response."));
          } else {
            return fromNull();
          }
        }
      };
    }
  }
}
