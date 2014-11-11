package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;

/**
 * User: aronen
 * Date: 7/24/13
 * Time: 5:35 PM
 */
public interface CacheLoader<K,V> {
  ComposableFuture<V> load(String cacheName, K key);
  ComposableFuture<Map<K, V>> load(String cacheName, Iterable<? extends K> keys);
}
