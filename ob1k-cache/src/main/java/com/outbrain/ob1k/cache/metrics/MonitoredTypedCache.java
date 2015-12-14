package com.outbrain.ob1k.cache.metrics;

import java.util.Map;
import com.google.common.base.Preconditions;
import com.outbrain.ob1k.cache.EntryMapper;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;

/**
 * A decorator adding metric monitoring to TypedCache.
 *
 * @author hunchback
 */
public class MonitoredTypedCache<K, V> implements TypedCache<K, V> {
  protected final TypedCache<K, V> inner;

  private final AsyncGetMetrics getAsyncMetrics;
  private final AsyncBulkGetMetrics getBulkAsyncMetric;
  private final AsyncOperationMetrics setAsyncMetrics;
  private final AsyncOperationMetrics setBulkAsyncMetrics;
  private final AsyncOperationMetrics deleteAsyncMetrics;

  public MonitoredTypedCache(final TypedCache<K, V> inner, String cacheName, final MetricFactory metricFactory) {
    Preconditions.checkNotNull(inner);
    this.inner = inner;
    Preconditions.checkNotNull(metricFactory);
    final String component = inner.getClass().getSimpleName() + "." + cacheName;

    getAsyncMetrics = new AsyncGetMetrics(metricFactory, component + ".getAsync");
    getBulkAsyncMetric = new AsyncBulkGetMetrics(metricFactory, component + ".getBulkAsync");
    setAsyncMetrics = new AsyncOperationMetrics(metricFactory, component + ".setAsync");
    setBulkAsyncMetrics = new AsyncOperationMetrics(metricFactory, component + ".setBulkAsync");
    deleteAsyncMetrics = new AsyncOperationMetrics(metricFactory, component + ".deleteAsync");
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    final Timer.Context timerCtx = getAsyncMetrics.start();
    ComposableFuture<V> future = inner.getAsync(key);
    future.consume(new Consumer<V>() {
      @Override
      public void consume(Try<V> res) {
       getAsyncMetrics.finished(res, timerCtx);
      }
    });
    return future;
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    final Timer.Context timer = getBulkAsyncMetric.start();
    ComposableFuture<Map<K, V>> future = inner.getBulkAsync(keys);
    future.consume(new Consumer<Map<K, V>>() {
      @Override
      public void consume(Try<Map<K, V>> res) {
        getBulkAsyncMetric.finished(res, timer);
        getBulkAsyncMetric.increaseTotal(keys);
      }
    });
    return future;
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    final Timer.Context timer = setAsyncMetrics.start();
    ComposableFuture<Boolean> future = inner.setAsync(key, value);
    future.consume(new Consumer<Boolean>() {
      @Override
      public void consume(Try<Boolean> res) {
        setAsyncMetrics.finished(res, timer);
      }
    });
    return future;
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    final Timer.Context timer = setAsyncMetrics.start();

    ComposableFuture<Boolean> future = inner.setAsync(key, mapper, maxIterations);
    future.consume(new Consumer<Boolean>() {
      @Override
      public void consume(Try<Boolean> res) {
        setAsyncMetrics.finished(res, timer);
      }
    });
    return future;
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    final Timer.Context timer = setBulkAsyncMetrics.start();
    ComposableFuture<Map<K, Boolean>> future = inner.setBulkAsync(entries);
    future.consume(new Consumer<Map<K, Boolean>>() {
      @Override
      public void consume(Try<Map<K, Boolean>> res) {
        setBulkAsyncMetrics.finished(res, timer);
      }
    });
    return future;
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    final Timer.Context timer = deleteAsyncMetrics.start();
    ComposableFuture<Boolean> future = inner.deleteAsync(key);
    future.consume(new Consumer<Boolean>() {
      @Override
      public void consume(Try<Boolean> res) {
        deleteAsyncMetrics.finished(res, timer);
      }
    });
    return future;
  }
}
