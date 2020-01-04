package com.outbrain.ob1k.cache;

import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Maps;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.eager.EagerComposableFuture;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.Map;
import java.util.concurrent.*;

import static com.outbrain.ob1k.concurrent.ComposableFutures.*;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * User: aronen
 * Date: 6/30/13
 * Time: 6:08 PM
 */
public class LocalAsyncCache<K, V> implements TypedCache<K, V> {

  private final AsyncLoadingCache<K, V> loadingCache;
  private final AsyncCache<K, V> localCache;
  private final String cacheName;

  public LocalAsyncCache(final com.outbrain.ob1k.cache.CacheConfiguration<K, V> cacheConfig) {
    final Caffeine<Object, Object> builder = Caffeine.newBuilder()
            .maximumSize(cacheConfig.getMaxSize())
            .expireAfterWrite(cacheConfig.getTtl(), cacheConfig.getTtlTimeUnit())
            .recordStats();

    cacheName = cacheConfig.getCacheName();
    if (cacheConfig.getLoader() != null) {
      this.localCache = null;
      this.loadingCache = builder.buildAsync(new InternalCacheLoader<>(cacheConfig.getLoader(), cacheName, cacheConfig.getFailOnMissingEntries()));
    } else {
      this.loadingCache = null;
      this.localCache = builder.buildAsync();
    }

    CaffeineCacheGaugesFactory.createGauges(cacheConfig.getMetricFactory(), cache().synchronous(), cacheName);
  }

  /**
   * @deprecated Replaced by {@link #LocalAsyncCache(com.outbrain.ob1k.cache.CacheConfiguration)}
   */
  @Deprecated
  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final CacheLoader<K, V> loader,
                         final MetricFactory metricFactory, final String cacheName, final boolean failOnMissingEntries) {
    this(new com.outbrain.ob1k.cache.CacheConfiguration<K, V>(cacheName)
            .withMetricFactory(metricFactory)
            .withMaxSize(maximumSize)
            .withTtl(ttl, unit)
            .withLoader(loader)
            .failOnMissingEntries(failOnMissingEntries));
  }



  /**
   * @deprecated Replaced by {link: #LocalAsyncCache(CacheConfiguration)}
   */
  @Deprecated
  public LocalAsyncCache(final int maximumSize, final int ttl, final TimeUnit unit, final MetricFactory metricFactory, final String cacheName) {
    this(new com.outbrain.ob1k.cache.CacheConfiguration<K, V>(cacheName)
            .withMetricFactory(metricFactory)
            .withMaxSize(maximumSize)
            .withTtl(ttl, unit));
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {

      try {
        if (loadingCache != null) {
          return EagerComposableFuture.
                  fromCompletableFuture(loadingCache.get(key)).
                  recoverWith(CompletionException.class, e -> ComposableFutures.fromError(e.getCause()));
        } else {
          final CompletableFuture<V> res = localCache.getIfPresent(key);
          if (res == null) {
            return fromNull();
          }
          return EagerComposableFuture.fromCompletableFuture(res);
        }
      } catch (final Exception e) {
        return fromError(e);
      }
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    return getAll(keys);
  }

  private ComposableFuture<Map<K, V>> getAll(Iterable<? extends K> keys) {
    return loadingCache == null ?
            fromValue(localCache.synchronous().getAllPresent(keys)) :
            EagerComposableFuture.fromCompletableFuture(loadingCache.getAll(keys)).
                    recoverWith(CompletionException.class, e -> ComposableFutures.fromError(e.getCause()));
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    cache().synchronous().invalidate(key);

    return fromValue(true);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    cache().put(key, completedFuture(value));

    return fromValue(true);
  }

  @Override
  public ComposableFuture<Boolean> setIfAbsentAsync(final K key, final V value) {
    return fromValue(cache().asMap().putIfAbsent(key, completedFuture(value)) == null);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    final ConcurrentMap<K, CompletableFuture<V>> map = cache().asMap();
    try {
      if (maxIterations == 0) {
        return fromValue(false);
      }

      final CompletableFuture<V> currentFuture = map.get(key);
      if (currentFuture != null) {
        return EagerComposableFuture.fromCompletableFuture(currentFuture).flatMap(currentValue -> {
          try {
            final V newValue = mapper.map(key, currentValue);
            if (newValue == null) {
              return fromValue(false);
            }

            final boolean success = map.replace(key, currentFuture, completedFuture(newValue));
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
          final CompletableFuture<V> prev = map.putIfAbsent(key, completedFuture(newValue));
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

  private AsyncCache<K, V> cache() {
    return loadingCache == null ? localCache : loadingCache;
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    final Map<K, ComposableFuture<Boolean>> result = Maps.newHashMapWithExpectedSize(entries.size());
    entries.forEach((k, v) -> result.put(k, setAsync(k, v)));

    return all(false, result);
  }

  private static class InternalCacheLoader<K, V> implements AsyncCacheLoader<K, V> {

    private final CacheLoader<K, V> loader;
    private final String cacheName;
    private final boolean failOnMissingEntries;

    private InternalCacheLoader(final CacheLoader<K, V> loader, final String cacheName, final boolean failOnMissingEntries) {
      this.loader = loader;
      this.cacheName = cacheName;
      this.failOnMissingEntries = failOnMissingEntries;
    }

    @Override
    public CompletableFuture<V> asyncLoad(final K key, final Executor executor) {
      return loader.load(cacheName, key).toCompletableFuture();
    }

    @Override
    public CompletableFuture<Map<K, V>> asyncLoadAll(final Iterable<? extends K> keys, final Executor executor) {
      if (failOnMissingEntries) {
        return loader.load(cacheName, keys).peek(map ->
                keys.forEach(key -> {
                  if (!map.containsKey(key)) {
                    throw new RuntimeException(key + " is missing from " + cacheName + " loader response.");
                  }
                })).toCompletableFuture();
      } else {
        return loader.load(cacheName, keys).toCompletableFuture();
      }
    }
  }
}
