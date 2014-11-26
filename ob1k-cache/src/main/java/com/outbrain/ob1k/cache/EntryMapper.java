package com.outbrain.ob1k.cache;

/**
 * mapping over an existing element in the cache and computing a vew value to be updated atomically.
 * returning a null value indicate no new value to update.
 *
 * @author aronen on 11/19/14.
 */
public interface EntryMapper<K, V> {
  V map(K key, V value);
}

