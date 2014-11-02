package com.outbrain.ob1k.cache;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.concurrent.handlers.OnErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.OnResultHandler;
import com.outbrain.ob1k.concurrent.handlers.OnSuccessHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.newPromise;

/**
 * a wrapper for TypedCache implementation that delegate missing entries to a loader.
 * the loader is used a such a way that prevents concurrent activations on the same key.
 *
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
    return cache.getAsync(key).continueOnSuccess(new FutureSuccessHandler<V, V>() {
      @Override
      public ComposableFuture<V> handle(V result) {
        if (result == null) {
          final ComposablePromise<V> promise = newPromise();
          final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
          if (prev != null) {
            return prev;
          }

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

          return promise;
        } else {
          return fromValue(result);
        }
      }
    });
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    final List<? extends K> listKeys = Lists.newArrayList(keys);
    return cache.getBulkAsync(listKeys).continueOnSuccess(new FutureSuccessHandler<Map<K, V>, Map<K, V>>() {
      @Override
      public ComposableFuture<Map<K, V>> handle(Map<K, V> result) {
        if (result == null || result.size() < listKeys.size()) {
          final Map<K, ComposablePromise<V>> missingEntries = new HashMap<>();
          final Map<K, ComposablePromise<V>> inProcessEntries = new HashMap<>();
          for (K key : listKeys) {
            if (result == null || !result.containsKey(key)) {
              final ComposablePromise<V> promise = newPromise();
              final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
              if (prev == null) {
//                System.out.println("key " + key + " is not in process");
                missingEntries.put(key, promise);
              } else {
//                System.out.println("key " + key + " is in process");
                inProcessEntries.put(key, prev);
              }
            }
          }

          if (!missingEntries.isEmpty()) {
            final ComposableFuture<Map<K, V>> loadedResults = loader.load(cacheName, missingEntries.keySet());
            loadedResults.onSuccess(new OnSuccessHandler<Map<K, V>>() {
              @Override
              public void handle(Map<K, V> elements) {
                for (K key : missingEntries.keySet()) {
                  missingEntries.get(key).set(elements.get(key));
                }

                cache.setBulkAsync(elements).onResult(new OnResultHandler<Map<K, Boolean>>() {
                  @Override
                  public void handle(ComposableFuture<Map<K, Boolean>> result) {
                    for (K key : missingEntries.keySet()) {
                      futureValues.remove(key, missingEntries.get(key));
                    }
                  }
                });
              }
            });

            loadedResults.onError(new OnErrorHandler() {
              @Override
              public void handle(Throwable error) {
                for (K key : missingEntries.keySet()) {
                  final ComposablePromise<V> promise = missingEntries.get(key);
                  promise.setException(error);
                  futureValues.remove(key, promise);
                }
              }
            });

          }

          final Map<K, ComposableFuture<V>> res = new HashMap<>();
          res.putAll(missingEntries);
          res.putAll(inProcessEntries);
          if (result != null) {
            for (K key: result.keySet()) {
              res.put(key, fromValue(result.get(key)));
            }
          }

          return ComposableFutures.all(false, res);

        } else {
          return fromValue(result);
        }
      }
    });

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
