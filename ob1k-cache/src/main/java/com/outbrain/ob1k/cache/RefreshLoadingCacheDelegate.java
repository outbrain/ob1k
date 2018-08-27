package com.outbrain.ob1k.cache;

import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * A wrapper of LoadingCacheDelegate that adds the ability to refresh cache values in the configured interval on access
 */
public class RefreshLoadingCacheDelegate<K, V> implements TypedCache<K, V> {

  private final LoadingCacheDelegate<K, ValueWithWriteTime<V>> cache;
  private final InternalCacheLoader internalCacheLoader;
  private final String cacheName;
  private final long loadDuration;
  private final TimeUnit loadTimeUnit;
  private final long refreshAfterWriteDuration;

  private final ConcurrentMap<K, Boolean> refreshingKeys;

  private final Counter refreshes;
  private final Counter refreshErrors;
  private final Counter refreshTimeouts;

  public RefreshLoadingCacheDelegate(final TypedCache<K, ValueWithWriteTime<V>> cache, final CacheLoader<K, V> loader, final String cacheName, final MetricFactory metricFactory,
                                     final long duration, final TimeUnit timeUnit, final boolean failOnError,
                                     final long refreshAfterWriteDuration, final TimeUnit refreshAfterWriteUnit) {
    this.cacheName = cacheName;
    this.internalCacheLoader = new InternalCacheLoader(loader);
    this.cache = new LoadingCacheDelegate<>(cache, internalCacheLoader, cacheName, metricFactory, duration, timeUnit, failOnError);
    this.loadDuration = duration;
    this.loadTimeUnit = timeUnit;
    this.refreshAfterWriteDuration = refreshAfterWriteUnit.toMillis(refreshAfterWriteDuration);

    refreshingKeys = new ConcurrentHashMap<>();

    if (metricFactory != null) {
      metricFactory.registerGauge("RefreshLoadingCacheDelegate." + cacheName, "refreshMapSize", refreshingKeys::size);

      refreshes = metricFactory.createCounter("RefreshLoadingCacheDelegate." + cacheName, "refreshes");
      refreshErrors = metricFactory.createCounter("RefreshLoadingCacheDelegate." + cacheName, "refreshErrors");
      refreshTimeouts = metricFactory.createCounter("RefreshLoadingCacheDelegate." + cacheName, "refreshTimeouts");
    } else {
      refreshes = null;
      refreshErrors = null;
      refreshTimeouts = null;
    }
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    ComposableFuture<ValueWithWriteTime<V>> futureValue = cache.getAsync(key);
    futureValue.consume(value -> {
      if (value.isSuccess() && shouldRefresh(value.getValue(), System.currentTimeMillis())) {
        this.refresh(key);
      }
    });
    return futureValue.map(ValueWithWriteTime::getValue);
  }

  private boolean shouldRefresh(final ValueWithWriteTime<V> value, long currentTimeMillis) {
    return value != null && currentTimeMillis - value.getWriteTime() >= refreshAfterWriteDuration;
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    ComposableFuture<Map<K, ValueWithWriteTime<V>>> resultMap = cache.getBulkAsync(keys);
    resultMap.consume(result -> {
      if (result.isSuccess()) {
        final List<K> keysToRefresh = collectKeysToRefresh(result.getValue(), System.currentTimeMillis());
        this.refresh(keysToRefresh);
      }
    });
    return extractValues(resultMap);
  }

  private List<K> collectKeysToRefresh(final Map<K, ValueWithWriteTime<V>> result, final long currentTimeMillis) {
    return result.entrySet().stream()
            .filter(entry -> shouldRefresh(entry.getValue(), currentTimeMillis))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
  }

  private ComposableFuture<Map<K, V>> extractValues(final ComposableFuture<Map<K, ValueWithWriteTime<V>>> valuesMap) {
    return valuesMap.map(map -> map.entrySet().stream()
            .filter(entry -> entry.getValue() != null && entry.getValue().getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getValue())));
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return cache.setAsync(key, new ValueWithWriteTime<>(value));
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return cache.setAsync(key, new InternalEntityMapper(mapper), maxIterations);
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    Map<K, ValueWithWriteTime<V>> newEntries = entries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ValueWithWriteTime<>(e.getValue())));
    return cache.setBulkAsync(newEntries);
  }

  @Override
  public ComposableFuture<Boolean> setIfAbsentAsync(final K key, final V value) {
    return cache.setIfAbsentAsync(key, new ValueWithWriteTime<>(value));
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    return cache.deleteAsync(key);
  }

  private class InternalCacheLoader implements CacheLoader<K, ValueWithWriteTime<V>> {

    private final CacheLoader<K, V> loader;

    private InternalCacheLoader(final CacheLoader<K, V> loader) {
      this.loader = loader;
    }

    @Override
    public ComposableFuture<ValueWithWriteTime<V>> load(final String cacheName, final K key) {
      return loader.load(cacheName, key).map(ValueWithWriteTime::new);
    }

    @Override
    public ComposableFuture<Map<K, ValueWithWriteTime<V>>> load(final String cacheName, final Iterable<? extends K> keys) {
      return loader.load(cacheName, keys).map(entries -> entries.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new ValueWithWriteTime<>(e.getValue()))));
    }
  }

  private class InternalEntityMapper implements EntryMapper<K, ValueWithWriteTime<V>> {
    private final EntryMapper<K, V> mapper;

    InternalEntityMapper(final EntryMapper<K, V> mapper) {
      this.mapper = mapper;
    }

    @Override
    public ValueWithWriteTime<V> map(final K key, final ValueWithWriteTime<V> value) {
      V extractedValue = value == null ? null : value.getValue();
      return new ValueWithWriteTime<>(mapper.map(key, extractedValue));
    }
  }

  /**
   * loads a fresh value from loader and sets into cache
   * @param key to refresh value from loader
   */
  private void refresh(final K key) {
    Boolean alreadyRefreshing = refreshingKeys.putIfAbsent(key, true);
    if (alreadyRefreshing == null) {
      incRefreshCount();
      internalCacheLoader.load(cacheName, key)
              .withTimeout(loadDuration, loadTimeUnit, "RefreshLoadingCacheDelegate fetch from loader; cache name: " + cacheName)
              .consume(res -> {
                refreshingKeys.remove(key);
                if (res.isSuccess()) {
                  cache.setAsync(key, res.getValue());
                } else {
                  collectRefreshErrorMetrics(res.getError());
                }
              });
    }
  }

  /**
   * loads fresh values from loader and sets into cache
   * @param keys to refresh value from loader
   */
  private void refresh(final Collection<K> keys) {
    if (keys == null || Iterables.isEmpty(keys)) {
      return;
    }

    final List<K> keysToRefresh = keys.stream()
            .filter(key -> null == refreshingKeys.putIfAbsent(key, true))
            .collect(Collectors.toList());

    if (!keysToRefresh.isEmpty()) {
      incRefreshCount();
      internalCacheLoader.load(cacheName, keysToRefresh)
              .withTimeout(loadDuration, loadTimeUnit, "RefreshLoadingCacheDelegate fetch bulk from loader; cache name: " + cacheName)
              .consume(res -> {
                keysToRefresh.forEach(refreshingKeys::remove);
                if (res.isSuccess()) {
                  cache.setBulkAsync(res.getValue());
                } else {
                  collectRefreshErrorMetrics(res.getError());
                }
              });
    }
  }

  private void incRefreshCount() {
    if (refreshes != null) {
      refreshes.inc();
    }
  }

  private void collectRefreshErrorMetrics(final Throwable error) {
    if (refreshErrors != null) {
      refreshErrors.inc();
      if (error instanceof TimeoutException) {
        refreshTimeouts.inc();
      }
    }
  }

  public static class Builder<K, V> {
    private static final long DEFAULT_LOAD_TIMEOUT_MS = 500;

    private final TypedCache<K, ValueWithWriteTime<V>> cache;
    private final CacheLoader<K, V> loader;
    private final String cacheName;
    private final MetricFactory metricFactory;

    private boolean failOnError = false;
    private long loadTimeout = DEFAULT_LOAD_TIMEOUT_MS;
    private TimeUnit loadTimeUnit = TimeUnit.MILLISECONDS;
    private long refreshAfterWriteDuration = -1;
    private TimeUnit refreshAfterWriteTimeUnit;

    public Builder(final TypedCache<K, ValueWithWriteTime<V>> cache, final CacheLoader<K, V> loader, final String cacheName, final MetricFactory metricFactory) {
      this.cache = cache;
      this.loader = loader;
      this.cacheName = cacheName;
      this.metricFactory = metricFactory;
    }

    public Builder<K, V> failOnError() {
      this.failOnError = true;
      return this;
    }

    public Builder<K, V> withLoadTimeout(final long timeout, final TimeUnit timeUnit) {
      this.loadTimeout = timeout;
      this.loadTimeUnit = timeUnit;
      return this;
    }

    public Builder<K, V> refreshAfterWrite(final long duration, final TimeUnit timeUnit) {
      this.refreshAfterWriteDuration = duration;
      this.refreshAfterWriteTimeUnit = timeUnit;
      return this;
    }

    public RefreshLoadingCacheDelegate<K, V> build() {
      if (refreshAfterWriteDuration == -1) {
        throw new IllegalArgumentException("missing refreshAfterWrite config");
      }
      return new RefreshLoadingCacheDelegate<>(cache, loader, cacheName, metricFactory, loadTimeout, loadTimeUnit, failOnError, refreshAfterWriteDuration, refreshAfterWriteTimeUnit);
    }
  }
}
