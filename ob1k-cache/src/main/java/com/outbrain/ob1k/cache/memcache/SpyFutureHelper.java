package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.BulkFuture;
import net.spy.memcached.internal.BulkGetFuture;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * User: aronen
 * Date: 9/18/13
 * Time: 11:34 AM
 */
public class SpyFutureHelper {
  public interface GetFutureProducer<T> {
    Future<T> createFuture();
  }

  public static <T> ComposableFuture<T> fromGet(final GetFutureProducer<T> source) {
    return ComposableFutures.build(consumer -> {
      try {
        final GetFuture<T> realFuture = (GetFuture<T>) source.createFuture();
        realFuture.addListener(future -> {
          try {
            final T value = realFuture.get();
            consumer.consume(Try.fromValue(value));
          } catch (final InterruptedException e) {
            consumer.consume(Try.fromError(e));
          } catch (final ExecutionException e) {
            consumer.consume(Try.fromError(e.getCause() == null ? e : e.getCause()));
          } catch (final Exception e) {
            consumer.consume(Try.fromError(e));
          }
        });
      } catch (final Exception e) {
        consumer.consume(Try.fromError(e));
      }
    });
  }

  public interface CASValueFutureProducer<T> {
    Future<CASValue<T>> createFuture();
  }

  public static <T> ComposableFuture<CASValue<T>> fromCASValue(final CASValueFutureProducer<T> source) {
    return ComposableFutures.build(consumer -> {
      final OperationFuture<CASValue<T>> realFuture = (OperationFuture<CASValue<T>>) source.createFuture();
      realFuture.addListener(future -> {
        try {
          @SuppressWarnings("unchecked")
          final CASValue<T> value = realFuture.get();
          consumer.consume(Try.fromValue(value));
        } catch (final InterruptedException e) {
          consumer.consume(Try.fromError(e));
        } catch (final ExecutionException e) {
          consumer.consume(Try.fromError(e.getCause() == null ? e : e.getCause()));
        } catch (final Exception e) {
          consumer.consume(Try.fromError(e));
        }
      });
    });
  }

  public interface CASFutureProducer {
    Future<CASResponse> createFuture();
  }

  public static ComposableFuture<CASResponse> fromCASResponse(final CASFutureProducer source) {
    return ComposableFutures.build(consumer -> {
      final OperationFuture<CASResponse> realFuture = (OperationFuture<CASResponse>) source.createFuture();
      realFuture.addListener(future -> {
        try {
          final CASResponse casResponse = realFuture.get();
          consumer.consume(Try.fromValue(casResponse));
        } catch (final InterruptedException e) {
          consumer.consume(Try.fromError(e));
        } catch (final ExecutionException e) {
          consumer.consume(Try.fromError(e.getCause() == null ? e : e.getCause()));
        } catch (final Exception e) {
          consumer.consume(Try.fromError(e));
        }
      });
    });
  }

  public interface BulkGetFutureProducer<V> {
    BulkFuture<Map<String, V>> createFuture();
  }

  public static <K, V> ComposableFuture<Map<K, V>> fromBulkGet(final BulkGetFutureProducer<V> source, final Map<String, K> keysMap) {
    return ComposableFutures.build(consumer -> {
      final BulkGetFuture<V> realFuture = (BulkGetFuture<V>) source.createFuture();
      realFuture.addListener(future -> {
        try {
          final Map<String, V> values = realFuture.get();
          final Map<K, V> translatedValues = new HashMap<>();
          for (final String key : values.keySet()) {
            final V value = values.get(key);
            translatedValues.put(keysMap.get(key), value);
          }

          consumer.consume(Try.fromValue(translatedValues));
        } catch (final InterruptedException e) {
          consumer.consume(Try.fromError(e));
        } catch (final ExecutionException e) {
          consumer.consume(Try.fromError(e.getCause() == null ? e : e.getCause()));
        } catch (final Exception e) {
          consumer.consume(Try.fromError(e));
        }
      });
    });
  }

  public interface OperationFutureProducer {
    Future<Boolean> createFuture();
  }

  public static ComposableFuture<Boolean> fromOperation(final OperationFutureProducer source) {
    return ComposableFutures.build(consumer -> {
      try {
        final OperationFuture<Boolean> realFuture = (OperationFuture<Boolean>) source.createFuture();
        realFuture.addListener(future -> {
          try {
            final Boolean value = realFuture.get();
            consumer.consume(Try.fromValue(value));
          } catch (final InterruptedException e) {
            consumer.consume(Try.fromError(e));
          } catch (final ExecutionException e) {
            consumer.consume(Try.fromError(e.getCause() == null ? e : e.getCause()));
          } catch (final Exception e) {
            consumer.consume(Try.fromError(e));
          }
        });
      } catch (final Exception e) {
        consumer.consume(Try.fromError(e));
      }
    });
  }

}
