package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;
import java.util.Objects;

/**
 * User: aronen
 * Date: 7/24/13
 * Time: 5:35 PM
 */
public interface CacheLoader<K,V> {
  ComposableFuture<V> load(String cacheName, K key);
  ComposableFuture<Map<K, V>> load(String cacheName, Iterable<? extends K> keys);


  /**
   * Creates a {@link CacheLoader} from a {@link TypedCache}
   * @param cache the wrapped cache
   * @param <K>
   * @param <V>
   * @return a CacheLoader wrapping the cache passed as argument
   */
  static <K, V> CacheLoader<K, V> fromTypedCache(final TypedCache<K, V> cache) {
    Objects.requireNonNull(cache, "cache must not be null");

    return new CacheLoader<K, V>() {
      @Override
      public ComposableFuture<V> load(final String cacheName, final K key) {
        return cache.getAsync(key);
      }

      @Override
      public ComposableFuture<Map<K, V>> load(final String cacheName, final Iterable<? extends K> keys) {
        return cache.getBulkAsync(keys);
      }
    };
  }
}
