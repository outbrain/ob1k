package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;

/**
 * Created by aronen on 8/27/14.
 *
 * an interface for local async cache with loader.
 */
public interface TypedCache<K, V> {
    ComposableFuture<V> getAsync(K key);
    ComposableFuture<Map<K, V>> getBulkAsync(Iterable<? extends K> keys);

    ComposableFuture<Boolean> setAsync(K key, V value);
    ComposableFuture<Boolean> setAsync(K key, V oldValue, V newValue);
    ComposableFuture<Map<K, Boolean>> setBulkAsync(Map<? extends K, ? extends V> entries);

    ComposableFuture<Boolean> deleteAsync(final K key);
}
