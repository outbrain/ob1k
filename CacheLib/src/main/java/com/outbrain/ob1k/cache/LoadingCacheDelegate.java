package com.outbrain.ob1k.cache;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.OnErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.OnResultHandler;
import com.outbrain.ob1k.concurrent.handlers.OnSuccessHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.outbrain.ob1k.concurrent.ComposableFutures.newPromise;

/**
 * a wrapper for TypedCache implementation that delegate missing entries to a loader.
 * the loader is used a such a way that prevents concurrent activations on the same key.
 * <p/>
 * Created by aronen on 10/26/14.
 */
public class LoadingCacheDelegate<K, V> implements TypedCache<K, V> {
  private final TypedCache<K, V> cache;
  private final CacheLoader<K, V> loader;
  private final String cacheName;
  private final ConcurrentMap<K, ComposablePromise<V>> futureValues;

  public LoadingCacheDelegate(TypedCache<K, V> cache, CacheLoader<K, V> loader, final String cacheName) {
    this.cache = cache;
    this.loader = loader;
    this.cacheName = cacheName;
    this.futureValues = new ConcurrentHashMap<>();
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    final ComposablePromise<V> promise = newPromise();
    final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
    if (prev != null) {
      return prev;
    }

    final ComposableFuture<V> cachedResult = cache.getAsync(key);
    cachedResult.onSuccess(new OnSuccessHandler<V>() {
      @Override
      public void handle(V result) {
        if (result == null) {

          final ComposableFuture<V> loadedResult = loader.load(cacheName, key);
          loadedResult.onSuccess(new OnSuccessHandler<V>() {
            @Override
            public void handle(V element) {
              promise.set(element);
              cache.setAsync(key, element).onResult(new OnResultHandler<Boolean>() {
                @Override
                public void handle(ComposableFuture<Boolean> result) {
                  futureValues.remove(key, promise);
                }
              });
            }
          });

          loadedResult.onError(new OnErrorHandler() {
            @Override
            public void handle(Throwable error) {
              promise.setException(error);
              futureValues.remove(key, promise);
            }
          });

        } else {
          promise.set(result);
        }
      }
    });

    cachedResult.onError(new OnErrorHandler() {
      @Override
      public void handle(Throwable error) {
        promise.setException(error);
        futureValues.remove(key, promise);
      }
    });

    return promise;
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    final List<? extends K> listKeys = Lists.newArrayList(keys);
    final List<K> processedKeys = new ArrayList<>();
    final Map<K, ComposableFuture<V>> res = new HashMap<>();

    for (K key : listKeys) {
      final ComposablePromise<V> promise = newPromise();
      final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
      if (prev == null) {
        res.put(key, promise);
        processedKeys.add(key);
      } else {
        res.put(key, prev);
      }
    }

    final ComposableFuture<Map<K, V>> cachedResults = cache.getBulkAsync(processedKeys);
    cachedResults.onSuccess(new OnSuccessHandler<Map<K, V>>() {
      @Override
      public void handle(Map<K, V> result) {
        final List<K> missingFromCacheKeys = new ArrayList<>();
        for (K key : processedKeys) {
          if (result.containsKey(key)) {
            final ComposablePromise<V> promise = futureValues.get(key);
            promise.set(result.get(key));
            futureValues.remove(key, promise);
          } else {
            missingFromCacheKeys.add(key);
          }
        }

        if (!missingFromCacheKeys.isEmpty()) {
          final ComposableFuture<Map<K, V>> loadedResults = loader.load(cacheName, missingFromCacheKeys);
          loadedResults.onSuccess(new OnSuccessHandler<Map<K, V>>() {
            @Override
            public void handle(Map<K, V> elements) {
              for (K key : missingFromCacheKeys) {
                futureValues.get(key).set(elements.get(key));
              }

              cache.setBulkAsync(elements).onResult(new OnResultHandler<Map<K, Boolean>>() {
                @Override
                public void handle(ComposableFuture<Map<K, Boolean>> result) {
                  for (K key : missingFromCacheKeys) {
                    futureValues.remove(key, res.get(key));
                  }
                }
              });
            }
          });

          loadedResults.onError(new OnErrorHandler() {
            @Override
            public void handle(Throwable error) {
              for (K key : missingFromCacheKeys) {
                final ComposablePromise<V> promise = futureValues.get(key);
                promise.setException(error);
                futureValues.remove(key, promise);
              }
            }
          });
        }
      }
    });

    cachedResults.onError(new OnErrorHandler() {
      @Override
      public void handle(Throwable error) {
        for (K key : processedKeys) {
          final ComposablePromise<V> promise = futureValues.get(key);
          promise.setException(error);
          futureValues.remove(key, promise);
        }
      }
    });

    return ComposableFutures.all(false, res);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(K key, V value) {
    return cache.setAsync(key, value);
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(Map<? extends K, ? extends V> entries) {
    return cache.setBulkAsync(entries);
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(K key) {
    return cache.deleteAsync(key);
  }
}
