package com.outbrain.ob1k.server.filters;

import com.outbrain.ob1k.AsyncRequestContext;
import com.outbrain.ob1k.SyncRequestContext;
import com.outbrain.ob1k.cache.LocalAsyncCache;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 10/28/14.
 *
 * a client/server filter that caches method call result for a predefined period of time.
 */
public class CachingFilter<K, V> implements AsyncFilter<V, AsyncRequestContext>, SyncFilter<V, SyncRequestContext> {
  private final TypedCache<K, V> cache;
  private final CacheKeyGenerator<K> generator;

  public CachingFilter(final CacheKeyGenerator<K> generator, int cacheSize, int ttl, TimeUnit timeUnit) {
    this.cache = new LocalAsyncCache<>(cacheSize, ttl, timeUnit);
    this.generator = generator;
  }

  @Override
  public ComposableFuture<V> handleAsync(final AsyncRequestContext ctx) {
    final K key = generator.createKey(ctx.getParams());
    return cache.getAsync(key).continueOnError(new ErrorHandler<V>() {
      @Override
      public V handle(Throwable error) throws ExecutionException {
        // in case there was an error in the cache we treat it as missing item in the cache.
        return null;
      }
    }).continueOnSuccess(new FutureSuccessHandler<V, V>() {
      @Override
      public ComposableFuture<V> handle(V result) {
        if (result != null) {
          return ComposableFutures.fromValue(result);
        } else {
          return ctx.<V>invokeAsync().continueOnSuccess(new SuccessHandler<V, V>() {
            @Override
            public V handle(V result) throws ExecutionException {
              cache.setAsync(key, result);
              return result;
            }
          });
        }
      }
    });
  }

  @Override
  public V handleSync(SyncRequestContext ctx) throws ExecutionException {
    final K key = generator.createKey(ctx.getParams());
    V cachedResult;
    try {
      cachedResult = cache.getAsync(key).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      cachedResult = null;
    } catch (ExecutionException e) {
      cachedResult = null;
    }

    if (cachedResult != null) {
      return cachedResult;
    } else {
      final V res = ctx.invokeSync();
      cache.setAsync(key, res);
      return res;
    }
  }

  public static interface CacheKeyGenerator<K> {
    public K createKey(Object[] params);
  }
}
