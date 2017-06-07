package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;

/**
 * Created by aronen on 8/27/14.
 *
 * an interface for local async cache with loader.
 */
public interface TypedCache<K, V> {
  ComposableFuture<V> getAsync(final K key);

  ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys);

  ComposableFuture<Boolean> setAsync(final K key, final V value);

  ComposableFuture<Boolean> setIfAbsentAsync(final K key, final V value);

  ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations);

  ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries);

  ComposableFuture<Boolean> deleteAsync(final K key);
}
