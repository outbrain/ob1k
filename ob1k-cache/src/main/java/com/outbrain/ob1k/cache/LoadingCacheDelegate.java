package com.outbrain.ob1k.cache;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.*;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;

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

  public LoadingCacheDelegate(final TypedCache<K, V> cache, final CacheLoader<K, V> loader, final String cacheName) {
    this.cache = cache;
    this.loader = loader;
    this.cacheName = cacheName;
    this.futureValues = new ConcurrentHashMap<>();
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    return ComposableFutures.build(new Producer<V>() {
      @Override
      public void produce(final Consumer<V> consumer) {
        final ComposablePromise<V> promise = newPromise();
        final ComposablePromise<V> prev = futureValues.putIfAbsent(key, promise);
        if (prev != null) {
          consumeFrom(prev.future(), consumer);
          return;
        }

        final ComposableFuture<V> cachedResult = cache.getAsync(key);
        cachedResult.consume(new Consumer<V>() {
          @Override
          public void consume(final Try<V> res) {
            if (res.isSuccess()) {
              final V result = res.getValue();
              if (result == null) {
                final ComposableFuture<V> loadedResult = loader.load(cacheName, key);
                loadedResult.consume(new Consumer<V>() {
                  @Override
                  public void consume(final Try<V> loadedRes) {
                    if (loadedRes.isSuccess()) {
                      promise.set(loadedRes.getValue());
                      cache.setAsync(key, loadedRes.getValue()).consume(new Consumer<Boolean>() {
                        @Override
                        public void consume(final Try<Boolean> result) {
                          futureValues.remove(key);
                        }
                      });
                    } else {
                      promise.setException(loadedRes.getError());
                      futureValues.remove(key);
                    }
                  }
                });
              } else {
                promise.set(result);
              }
            } else {
              promise.setException(res.getError());
              futureValues.remove(key);
            }
          }
        });

        consumeFrom(promise.future(), consumer);
      }
    });
  }

  private static <T> void consumeFrom(final ComposableFuture<T> source, final Consumer<T> consumer) {
    source.consume(new Consumer<T>() {
      @Override
      public void consume(final Try<T> result) {
        consumer.consume(result);
      }
    });
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    return ComposableFutures.build(new Producer<Map<K, V>>() {
      @Override
      public void produce(final Consumer<Map<K, V>> consumer) {
        final List<? extends K> listKeys = Lists.newArrayList(keys);
        final List<K> processedKeys = new ArrayList<>();
        final Map<K, ComposablePromise<V>> res = new HashMap<>();

        for (final K key : listKeys) {
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
        cachedResults.consume(new Consumer<Map<K, V>>() {
          @Override
          public void consume(final Try<Map<K, V>> res) {
            if (res.isSuccess()) {
              final Map<K, V> result = res.getValue();
              final List<K> missingFromCacheKeys = new ArrayList<>();
              for (final K key : processedKeys) {
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
                loadedResults.consume(new Consumer<Map<K, V>>() {
                  @Override
                  public void consume(final Try<Map<K, V>> loadedRes) {
                    if (loadedRes.isSuccess()) {
                      final Map<K, V> elements = loadedRes.getValue();
                      for (final K key : missingFromCacheKeys) {
                        futureValues.get(key).set(elements.get(key));
                      }

                      cache.setBulkAsync(elements).consume(new Consumer<Map<K, Boolean>>() {
                        @Override
                        public void consume(final Try<Map<K, Boolean>> setResults) {
                          for (final K key : missingFromCacheKeys) {
                            futureValues.remove(key);
                          }
                        }
                      });

                    } else {
                      for (final K key : missingFromCacheKeys) {
                        final ComposablePromise<V> promise = futureValues.get(key);
                        promise.setException(loadedRes.getError());
                        futureValues.remove(key);
                      }
                    }
                  }
                });
              }
            } else {
              for (final K key : processedKeys) {
                final ComposablePromise<V> promise = futureValues.get(key);
                promise.setException(res.getError());
                futureValues.remove(key);
              }
            }
          }
        });

        consumeFrom(ComposableFutures.all(false, mapToFutures(res)), consumer);
      }
    });
  }

  private static <K, V> Map<K, ComposableFuture<V>> mapToFutures(final Map<K, ComposablePromise<V>> promises) {
    final HashMap<K, ComposableFuture<V>> result = new HashMap<>(promises.size());
    for (final Map.Entry<K, ComposablePromise<V>> promiseEntry : promises.entrySet()) {
      result.put(promiseEntry.getKey(), promiseEntry.getValue().future());
    }

    return result;
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return cache.setAsync(key, value);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return cache.setAsync(key, mapper, maxIterations);
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    return cache.setBulkAsync(entries);
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    return cache.deleteAsync(key);
  }
}
