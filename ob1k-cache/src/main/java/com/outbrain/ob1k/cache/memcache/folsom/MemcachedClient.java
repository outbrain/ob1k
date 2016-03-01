package com.outbrain.ob1k.cache.memcache.folsom;

import com.google.common.util.concurrent.ListenableFuture;
import com.outbrain.ob1k.cache.EntryMapper;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheStatus;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by eran on 2/23/16.
 */
public class MemcachedClient<K, V> implements TypedCache<K, V> {

  private final MemcacheClient<V> folsomClient;
  private final CacheKeyTranslator<K> keyTranslator;
  private final int expirationSeconds;
  // TODO should we create a dedicated executor?
  private final Executor executor = ComposableFutures.getExecutor();

  // TODO add docs (especially about the expiration rules)
  public MemcachedClient(final MemcacheClient<V> folsomClient, final CacheKeyTranslator<K> keyTranslator, final long expiration, final TimeUnit timeUnit) {
    this.folsomClient = Objects.requireNonNull(folsomClient, "folsomClient must not be null");
    this.keyTranslator = Objects.requireNonNull(keyTranslator, "keyTranslator must not be null");
    this.expirationSeconds = (int) timeUnit.toSeconds(expiration);
  }

  @Override
  public ComposableFuture<V> getAsync(final K key) {
    return fromListenableFuture(() -> folsomClient.get(key(key)));
  }

  @Override
  public ComposableFuture<Map<K, V>> getBulkAsync(final Iterable<? extends K> keys) {
    return null;
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final V value) {
    return fromListenableFuture(() -> folsomClient.set(key(key), value, expirationSeconds), this::isOK);
  }

  @Override
  public ComposableFuture<Boolean> setAsync(final K key, final EntryMapper<K, V> mapper, final int maxIterations) {
    return null;
  }

  @Override
  public ComposableFuture<Map<K, Boolean>> setBulkAsync(final Map<? extends K, ? extends V> entries) {
    return null;
  }

  @Override
  public ComposableFuture<Boolean> deleteAsync(final K key) {
    return fromListenableFuture(() -> folsomClient.delete(key(key)), this::isOK);
  }


  private String key(final K key) {
    return keyTranslator.translateKey(key);
  }

  private boolean isOK(final MemcacheStatus memcacheStatus) {
    return memcacheStatus == MemcacheStatus.OK;
  }

  private interface Provider<T> {
    ListenableFuture<T> provide();
  }

  private <T> ComposableFuture<T> fromListenableFuture(final Provider<T> provider) {
    return fromListenableFuture(provider, Function.identity());
  }

  private <T, R> ComposableFuture<R> fromListenableFuture(final Provider<T> provider, final Function<T, R> resultTransformer) {

    return ComposableFutures.build(consumer -> {
      try {
        final ListenableFuture<T> source = provider.provide();
        source.addListener(() -> {
          try {
            final R result = resultTransformer.apply(source.get());
            consumer.consume(Try.fromValue(result));
          } catch (final InterruptedException e) {
            consumer.consume(Try.fromError(e));
          } catch (final ExecutionException e) {
            final Throwable error = e.getCause() != null ? e.getCause() : e;
            consumer.consume(Try.fromError(error));
          }
        }, executor);
      } catch (final Exception e) {
        consumer.consume(Try.fromError(e));
      }
    });
  }
}
