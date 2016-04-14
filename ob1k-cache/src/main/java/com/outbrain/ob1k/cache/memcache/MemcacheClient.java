package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.cache.EntryMapper;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.transcoders.Transcoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * Created by aronen on 10/12/14.
 * a thin wrapper around spy memcache client.
 * it creates a typed "view" over the content of the cache with predefined expiration for all entries in it.
 * <p/>
 * all operations are async and return ComposableFuture.
 */
public class MemcacheClient<K, V> implements TypedCache<K, V> {
  private static final long MAX_EXPIRATION_SEC = 60 * 60 * 24 * 30;

  private final MemcachedClientIF spyClient;
  private final CacheKeyTranslator<K> keyTranslator;
  private final int expirationSpyUnits;

  public MemcacheClient(final MemcachedClientIF spyClient, final CacheKeyTranslator<K> keyTranslator, final long expiration, final TimeUnit timeUnit) {
    this.spyClient = spyClient;
    this.keyTranslator = keyTranslator;
    this.expirationSpyUnits = expirationInSpyUnits(expiration, timeUnit);
  }

  /**
   * from spy documentation:
   * <p/>
   * The actual (exp)value sent may either be Unix time (number of seconds since
   * January 1, 1970, as a 32-bit value), or a number of seconds starting from
   * current time. In the latter case, this number of seconds may not exceed
   * 60*60*24*30 (number of seconds in 30 days); if the number sent by a client
   * is larger than that, the server will consider it to be real Unix time value
   * rather than an offset from current time.
   *
   * @param exp  expiration in unit units.
   * @param unit the time unit.
   * @return the expiration in secs according to memcached format.
   */
  private static int expirationInSpyUnits(final long exp, final TimeUnit unit) {
    final long intervalSec = unit.toSeconds(exp);
    return (int) (intervalSec <= MAX_EXPIRATION_SEC ? intervalSec : (System.currentTimeMillis() / 1000) + intervalSec);
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    return SpyFutureHelper.fromGet(() -> {
      @SuppressWarnings("unchecked")
      final Transcoder<V> transcoder = (Transcoder<V>) spyClient.getTranscoder();
      return spyClient.asyncGet(keyTranslator.translateKey(key), transcoder);
    });
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

      return SpyFutureHelper.fromBulkGet(() -> {
        @SuppressWarnings("unchecked")
        final Transcoder<V> transcoder = (Transcoder<V>) spyClient.getTranscoder();
        return spyClient.asyncGetBulk(stringKeys, transcoder);
      }, keysMap);

    } catch (final Exception e) {
      return fromError(e);
    }
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return SpyFutureHelper.fromOperation(() -> spyClient.set(keyTranslator.translateKey(key), expirationSpyUnits, value));
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return casUpdate(key, mapper).continueOnSuccess((FutureSuccessHandler<CASResponse, Boolean>) result -> {
      if (result == CASResponse.OK || result == CASResponse.OBSERVE_MODIFIED) {
        return fromValue(true);
      }

      if (maxIterations > 0 && result == CASResponse.EXISTS) {
        return setAsync(key, mapper, maxIterations - 1);
      }

      return fromValue(false);
    });
  }

  private ComposableFuture<CASResponse> casUpdate(final K key, final EntryMapper<K, V> mapper) {
    try {
      final String cacheKey = keyTranslator.translateKey(key);
      final ComposableFuture<CASValue<V>> getFutureValue = SpyFutureHelper.fromCASValue(() -> {
        @SuppressWarnings("unchecked")
        final Transcoder<V> transcoder = (Transcoder<V>) spyClient.getTranscoder();
        return spyClient.asyncGets(cacheKey, transcoder);
      });

      return getFutureValue.continueOnSuccess((FutureSuccessHandler<CASValue<V>, CASResponse>) result -> {
        final V newValue = result == null ? mapper.map(key, null) : mapper.map(key, result.getValue());
        if (newValue == null) {
          return fromValue(CASResponse.OBSERVE_ERROR_IN_ARGS);
        }

        if (result != null) {
          return SpyFutureHelper.fromCASResponse(() -> spyClient.asyncCAS(cacheKey, result.getCas(), newValue));

        } else {
          final ComposableFuture<Boolean> addResponse = SpyFutureHelper.fromOperation(
            () -> spyClient.add(cacheKey, expirationSpyUnits, newValue));

          return addResponse.continueOnSuccess((SuccessHandler<Boolean, CASResponse>) result1 -> {
            if (result1 == Boolean.TRUE) {
              return CASResponse.OK;
            } else {
              return CASResponse.EXISTS;
            }
          });
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
    return SpyFutureHelper.fromOperation(() -> spyClient.delete(keyTranslator.translateKey(key)));
  }
}
