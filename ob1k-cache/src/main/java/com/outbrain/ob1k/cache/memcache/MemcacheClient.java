package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.internal.BulkFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 10/12/14.
 * a thin wrapper around spy memcache client.
 * it creates a typed "view" over the content of the cache with predefined expiration for all entries in it.
 *
 * all operations are async and return ComposableFuture.
 */
public class MemcacheClient<K, V> implements TypedCache<K, V> {
  private final MemcachedClientIF spyClient;
  private final CacheKeyTranslator<K> keyTranslator;
  private final int expirationMs;

  public MemcacheClient(MemcachedClientIF spyClient, final CacheKeyTranslator<K> keyTranslator, long expiration, TimeUnit timeUnit) {
    this.spyClient = spyClient;
    this.keyTranslator = keyTranslator;
    this.expirationMs = (int) timeUnit.toMillis(expiration);
  }

  @Override
  public ComposableFuture<V> getAsync(K key) {
    return SpyFutureHelper.fromGet(spyClient.asyncGet(keyTranslator.translateKey(key)));
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(Iterable<? extends K> keys) {
    final List<String> stringKeys = new ArrayList<>();
    Map<String, K> keysMap = new HashMap<>();
    for (K key : keys) {
      final String stringKey = keyTranslator.translateKey(key);
      stringKeys.add(stringKey);
      keysMap.put(stringKey, key);
    }

    final BulkFuture<Map<String, Object>> res = spyClient.asyncGetBulk(stringKeys);
    return SpyFutureHelper.fromBulkGet(res, keysMap);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(K key, V value) {
    final Future<Boolean> setRes = spyClient.set(keyTranslator.translateKey(key), expirationMs, value);
    return SpyFutureHelper.fromOperation(setRes);
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(Map<? extends K, ? extends V> entries) {
    Map<K, ComposableFuture<Boolean>> results = new HashMap<>();
    for (K key : entries.keySet()) {
      final ComposableFuture<Boolean> singleRes = setAsync(key, entries.get(key));
      results.put(key, singleRes);
    }

    return ComposableFutures.all(false, results);
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(K key) {
    return SpyFutureHelper.fromOperation(spyClient.delete(keyTranslator.translateKey(key)));
  }
}
