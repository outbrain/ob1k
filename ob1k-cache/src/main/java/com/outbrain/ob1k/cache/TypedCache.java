package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.Map;

/**
 * Ob1k's typed interface for async cache implementations.
 * All implementations are async using ComposableFuture to represent the result.
 *
 * @author aronen
 */
public interface TypedCache<K, V> {

  /**
   * Retrieves a value for a given key.
   *
   * @param key cache key
   * @return future with cache result
   */
  ComposableFuture<V> getAsync(K key);

  /**
   * Retrieves multiple values for a given collection of keys.
   *
   * @param keys collection of keys
   * @return future with map of cache results
   */
  ComposableFuture<Map<K, V>> getBulkAsync(Iterable<? extends K> keys);

  /**
   * Sets a new value for a given key.
   * Overrides in case the key were already existing.
   *
   * @param key   cache key
   * @param value cache value
   * @return future with boolean represents operation result
   */
  ComposableFuture<Boolean> setAsync(K key, V value);

  /**
   * Atomically sets a new value by transforming the current value into a new one.
   * In case the CAS operation failed, the mapper will be called again and again with the new read value,
   * until max iterations reach its limit.
   *
   * @param key           cache key
   * @param mapper        cache value mapper
   * @param maxIterations max iterations
   * @return future with boolean represents operation result
   */
  ComposableFuture<Boolean> setAsync(K key, EntryMapper<K, V> mapper, int maxIterations);

  /**
   * Sets new values in bulk operation for a given map of (k, v) pairs.
   * Overrides in case the key were already existing.
   * <p>
   * NOTE: This operation is not guaranteed to be atomic, and depends on the implementation.
   *
   * @param entries cache entries
   * @return future with boolean represents operation result per each key
   */
  ComposableFuture<Map<K, Boolean>> setBulkAsync(Map<? extends K, ? extends V> entries);

  /**
   * Sets a new value for a given key, only if no previous value is set for the key.
   *
   * @param key   cache key
   * @param value cache value
   * @return future with true in case the set operation succeed, or false if the key's already exists
   */
  ComposableFuture<Boolean> setIfAbsentAsync(K key, V value);

  /**
   * Delete (invalidate) key from cache by key.
   *
   * @param key cache key
   * @return future with boolean represents operation result
   */
  ComposableFuture<Boolean> deleteAsync(K key);
}
