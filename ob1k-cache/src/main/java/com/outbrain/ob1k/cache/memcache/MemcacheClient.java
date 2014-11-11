package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.internal.BulkFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;

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

  public MemcacheClient(final MemcachedClientIF spyClient, final CacheKeyTranslator<K> keyTranslator, final long expiration, final TimeUnit timeUnit) {
    this.spyClient = spyClient;
    this.keyTranslator = keyTranslator;
    this.expirationMs = (int) timeUnit.toMillis(expiration);
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    try {
      return SpyFutureHelper.fromGet(spyClient.asyncGet(keyTranslator.translateKey(key)));
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    final List<String> stringKeys = new ArrayList<>();
    final Map<String, K> keysMap = new HashMap<>();
    try {
      for (final K key : keys) {
        final String stringKey = keyTranslator.translateKey(key);
        stringKeys.add(stringKey);
        keysMap.put(stringKey, key);
      }

      final BulkFuture<Map<String, Object>> res = spyClient.asyncGetBulk(stringKeys);
      return SpyFutureHelper.fromBulkGet(res, keysMap);
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    try {
      final Future<Boolean> setRes = spyClient.set(keyTranslator.translateKey(key), expirationMs, value);
      return SpyFutureHelper.fromOperation(setRes);
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V oldValue, final V newValue) {
    try {
      final String cacheKey = keyTranslator.translateKey(key);
      final Future<CASValue<Object>> getFutureValue = spyClient.asyncGets(cacheKey);
      return SpyFutureHelper.<V>fromCASValue(getFutureValue).continueOnSuccess(new FutureSuccessHandler<CASValue<V>, CASResponse>() {
        @Override
        public ComposableFuture<CASResponse> handle(final CASValue<V> result) {
          try {
            return SpyFutureHelper.fromCASResponse(spyClient.asyncCAS(cacheKey, result.getCas(), result.getValue()));
          } catch (final Exception e) {
            return fromError(e);
          }
        }
      }).continueOnSuccess(new SuccessHandler<CASResponse, Boolean>() {
        @Override
        public Boolean handle(final CASResponse result) {
          return result == CASResponse.OK;
        }
      });
    } catch (final Exception e) {
      return fromError(e);
    }
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    final Map<K, ComposableFuture<Boolean>> results = new HashMap<>();
    for (final K key : entries.keySet()) {
      final ComposableFuture<Boolean> singleRes = setAsync(key, entries.get(key));
      results.put(key, singleRes);
    }

    return ComposableFutures.all(false, results);
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    try {
      return SpyFutureHelper.fromOperation(spyClient.delete(keyTranslator.translateKey(key)));
    } catch (final Exception e) {
      return fromError(e);
    }
  }
}
