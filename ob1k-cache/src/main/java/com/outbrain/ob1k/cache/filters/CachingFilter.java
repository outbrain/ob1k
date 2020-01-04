package com.outbrain.ob1k.cache.filters;

import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.cache.CacheConfiguration;
import com.outbrain.ob1k.cache.LocalAsyncCache;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;

import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 10/28/14.
 *
 * a client/server filter that caches method call result for a predefined period of time.
 */
public class CachingFilter<K, V> implements AsyncFilter<V, AsyncRequestContext> {
  private final TypedCache<K, V> cache;
  private final CacheKeyGenerator<K> generator;

  /**
   * @deprecated Replaced by {link: #CachingFilter(CacheKeyGenerator, CacheConfiguration)}
   */
  public CachingFilter(final CacheKeyGenerator<K> generator, int cacheSize, int ttl, TimeUnit timeUnit) {
    this.cache = new LocalAsyncCache<>(cacheSize, ttl, timeUnit, null, "default");
    this.generator = generator;
  }

  public CachingFilter(final CacheKeyGenerator<K> generator, final CacheConfiguration cacheConfiguration) {
    this.cache = new LocalAsyncCache<>(cacheConfiguration);
    this.generator = generator;
  }

  @Override
  public ComposableFuture<V> handleAsync(final AsyncRequestContext ctx) {
    final K key = generator.createKey(ctx.getParams());
    return cache.getAsync(key).recover(error -> {
      // in case there was an error in the cache we treat it as missing item in the cache.
      return null;
    }).flatMap(result -> {
      if (result != null) {
        return ComposableFutures.fromValue(result);
      } else {
        return ctx.<V>invokeAsync().map(result1 -> {
          cache.setAsync(key, result1);
          return result1;
        });
      }
    });
  }

  public interface CacheKeyGenerator<K> {
    K createKey(Object[] params);
  }
}
