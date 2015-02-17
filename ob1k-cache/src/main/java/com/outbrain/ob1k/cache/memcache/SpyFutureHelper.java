package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.concurrent.*;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.internal.*;

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
  public static interface GetFutureProducer<T> {
    Future<T> createFuture();
  }

  public static <T> ComposableFuture<T> fromGet(final GetFutureProducer<T> source) {
    return ComposableFutures.build(new Producer<T>() {
      @Override
      public void produce(final Consumer<T> consumer) {
        try {
          final GetFuture<T> realFuture = (GetFuture<T>) source.createFuture();
          realFuture.addListener(new GetCompletionListener() {
            @Override
            public void onComplete(final GetFuture<?> future) throws Exception {
              try {
                final T value = realFuture.get();
                consumer.consume(Try.fromValue(value));
              } catch (final InterruptedException e) {
                consumer.consume(Try.<T>fromError(e));
              } catch (final ExecutionException e) {
                consumer.consume(Try.<T>fromError(e.getCause() == null ? e : e.getCause()));
              } catch (final Exception e) {
                consumer.consume(Try.<T>fromError(e));
              }
            }
          });
        } catch (final Exception e) {
          consumer.consume(Try.<T>fromError(e));
        }
      }
    });
  }

  public static interface CASValueFutureProducer<T> {
    Future<CASValue<T>> createFuture();
  }

  public static <T> ComposableFuture<CASValue<T>> fromCASValue(final CASValueFutureProducer<T> source) {
    return ComposableFutures.build(new Producer<CASValue<T>>() {
      @Override
      public void produce(final Consumer<CASValue<T>> consumer) {
        final OperationFuture<CASValue<T>> realFuture = (OperationFuture<CASValue<T>>) source.createFuture();
        realFuture.addListener(new OperationCompletionListener() {
          @Override
          public void onComplete(final OperationFuture<?> future) throws Exception {
            try {
              @SuppressWarnings("unchecked")
              final CASValue<T> value = realFuture.get();
              consumer.consume(Try.fromValue(value));
            } catch (final InterruptedException e) {
              consumer.consume(Try.<CASValue<T>>fromError(e));
            } catch (final ExecutionException e) {
              consumer.consume(Try.<CASValue<T>>fromError(e.getCause() == null ? e : e.getCause()));
            } catch (final Exception e) {
              consumer.consume(Try.<CASValue<T>>fromError(e));
            }
          }
        });
      }
    });
  }

  public static interface CASFutureProducer {
    Future<CASResponse> createFuture();
  }

  public static ComposableFuture<CASResponse> fromCASResponse(final CASFutureProducer source) {
    return ComposableFutures.build(new Producer<CASResponse>() {
      @Override
      public void produce(final Consumer<CASResponse> consumer) {
        final OperationFuture<CASResponse> realFuture = (OperationFuture<CASResponse>) source.createFuture();
        realFuture.addListener(new OperationCompletionListener() {
          @Override
          public void onComplete(final OperationFuture<?> future) throws Exception {
            try {
              final CASResponse casResponse = realFuture.get();
              consumer.consume(Try.fromValue(casResponse));
            } catch (final InterruptedException e) {
              consumer.consume(Try.<CASResponse>fromError(e));
            } catch (final ExecutionException e) {
              consumer.consume(Try.<CASResponse>fromError(e.getCause() == null ? e : e.getCause()));
            } catch (final Exception e) {
              consumer.consume(Try.<CASResponse>fromError(e));
            }
          }
        });
      }
    });
  }

  public static interface BulkGetFutureProducer<V> {
    BulkFuture<Map<String, V>> createFuture();
  }

  public static <K, V> ComposableFuture<Map<K, V>> fromBulkGet(final BulkGetFutureProducer<V> source, final Map<String, K> keysMap) {
    return ComposableFutures.build(new Producer<Map<K, V>>() {
      @Override
      public void produce(final Consumer<Map<K, V>> consumer) {
        final BulkGetFuture<V> realFuture = (BulkGetFuture<V>) source.createFuture();
        realFuture.addListener(new BulkGetCompletionListener() {
          @Override
          public void onComplete(final BulkGetFuture<?> future) throws Exception {
            try {
              final Map<String, V> values = realFuture.get();
              final Map<K, V> translatedValues = new HashMap<>();
              for (final String key : values.keySet()) {
                final V value = values.get(key);
                translatedValues.put(keysMap.get(key), value);
              }

              consumer.consume(Try.fromValue(translatedValues));
            } catch (final InterruptedException e) {
              consumer.consume(Try.<Map<K, V>>fromError(e));
            } catch (final ExecutionException e) {
              consumer.consume(Try.<Map<K, V>>fromError(e.getCause() == null ? e : e.getCause()));
            } catch (final Exception e) {
              consumer.consume(Try.<Map<K, V>>fromError(e));
            }
          }
        });
      }
    });
  }

  public static interface OperationFutureProducer {
    Future<Boolean> createFuture();
  }

  public static ComposableFuture<Boolean> fromOperation(final OperationFutureProducer source) {
    return ComposableFutures.build(new Producer<Boolean>() {
      @Override
      public void produce(final Consumer<Boolean> consumer) {
        try {
          final OperationFuture<Boolean> realFuture = (OperationFuture<Boolean>) source.createFuture();
          realFuture.addListener(new OperationCompletionListener() {
            @Override
            public void onComplete(final OperationFuture<?> future) throws Exception {
              try {
                final Boolean value = realFuture.get();
                consumer.consume(Try.fromValue(value));
              } catch (final InterruptedException e) {
                consumer.consume(Try.<Boolean>fromError(e));
              } catch (final ExecutionException e) {
                consumer.consume(Try.<Boolean>fromError(e.getCause() == null ? e : e.getCause()));
              } catch (final Exception e) {
                consumer.consume(Try.<Boolean>fromError(e));
              }
            }
          });
        } catch (final Exception e) {
          consumer.consume(Try.<Boolean>fromError(e));
        }
      }
    });
  }

}
