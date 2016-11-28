package com.outbrain.ob1k.cache.metrics;

import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Iterables;
import com.outbrain.ob1k.cache.EntryMapper;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A decorator adding metric monitoring to TypedCache.
 *
 * @author hunchback
 */
public class MonitoringCacheDelegate<K, V> implements TypedCache<K, V> {
  protected final TypedCache<K, V> delegate;

  /* interface call metrics */
  private final AsyncOperationMetrics<V> getAsyncMetrics;
  private final AsyncOperationMetrics<Map<K, V>> getBulkAsyncMetrics;
  private final AsyncOperationMetrics<Boolean> setAsyncMetrics;
  private final AsyncOperationMetrics<Map<K, Boolean>> setBulkAsyncMetrics;
  private final AsyncOperationMetrics<Boolean> deleteAsyncMetrics;

  /* cache metrics */
  private final Counter total;
  private final Counter hits;

  private final Function<V, V> getCacheMetricsUpdater = new Function<V, V>() {
    @Override
    public V apply(final V result) {
      total.inc();
      if (result != null) {
        hits.inc();
      }
      return result;
    }
  };

  public MonitoringCacheDelegate(final TypedCache<K, V> delegate, final String cacheName, final MetricFactory metricFactory) {
    checkNotNull(metricFactory, "metricFactory may not be null");
    this.delegate = checkNotNull(delegate, "delegate may not be null");
    final String component = delegate.getClass().getSimpleName() + "." + cacheName;

    getAsyncMetrics = new AsyncOperationMetrics<>(metricFactory, component + ".getAsync");
    getBulkAsyncMetrics = new AsyncOperationMetrics<>(metricFactory, component + ".getBulkAsync");
    setAsyncMetrics = new AsyncOperationMetrics<>(metricFactory, component + ".setAsync");
    setBulkAsyncMetrics = new AsyncOperationMetrics<>(metricFactory, component + ".setBulkAsync");
    deleteAsyncMetrics = new AsyncOperationMetrics<>(metricFactory, component + ".deleteAsync");

    final String componentCache = component + ".Cache";
    hits = metricFactory.createCounter(componentCache, "hits");
    total = metricFactory.createCounter(componentCache, "total");
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    return getAsyncMetrics.update(delegate.getAsync(key))
            .map(getCacheMetricsUpdater);
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    return getBulkAsyncMetrics.update(delegate.getBulkAsync(keys))
      .map(result -> {
        total.inc(Iterables.size(keys));
        hits.inc(result.size());
        return result;
      });
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return setAsyncMetrics.update(delegate.setAsync(key, value));
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return setAsyncMetrics.update(delegate.setAsync(key, mapper, maxIterations));
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    return setBulkAsyncMetrics.update(delegate.setBulkAsync(entries));
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    return deleteAsyncMetrics.update(delegate.deleteAsync(key));
  }
}
