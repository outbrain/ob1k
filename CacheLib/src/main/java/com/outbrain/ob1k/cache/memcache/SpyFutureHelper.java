package com.outbrain.ob1k.cache.memcache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.ComposablePromise;
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
  public static <T> ComposableFuture<T> fromGet(final Future<Object> source) {
    GetFuture<Object> realFuture = (GetFuture<Object>) source;
    final ComposablePromise<T> res = ComposableFutures.newPromise();
    realFuture.addListener(new GetCompletionListener() {
      @Override
      public void onComplete(GetFuture<?> future) throws Exception {
        try {
          @SuppressWarnings("unchecked")
          T value = (T) source.get();
          res.set(value);
        } catch (InterruptedException e) {
          res.setException(e);
        } catch (ExecutionException e) {
          res.setException(e.getCause() == null ? e : e.getCause());
        } catch (Exception e) {
          res.setException(e);
        }
      }
    });

    return res;
  }

  public static <K, V> ComposableFuture<Map<K, V>> fromBulkGet(final BulkFuture<Map<String, Object>> source, final Map<String, K> keysMap) {
    BulkGetFuture<Object> realFuture = (BulkGetFuture<Object>) source;
    final ComposablePromise<Map<K, V>> res = ComposableFutures.newPromise();
    realFuture.addListener(new BulkGetCompletionListener() {
      @Override
      public void onComplete(BulkGetFuture<?> future) throws Exception {
        try {
          @SuppressWarnings("unchecked")
          Map<String, Object> values = source.get();
          final HashMap<K, V> translatedValues = new HashMap<>();
          for (String key : values.keySet()) {
            @SuppressWarnings("unchecked")
            final V value = (V) values.get(key);
            translatedValues.put(keysMap.get(key), value);
          }

          res.set(translatedValues);
        } catch (InterruptedException e) {
          res.setException(e);
        } catch (ExecutionException e) {
          res.setException(e.getCause() == null ? e : e.getCause());
        } catch (Exception e) {
          res.setException(e);
        }
      }
    });

    return res;
  }

  public static ComposableFuture<Boolean> fromOperation(final Future<Boolean> source) {
    OperationFuture<Boolean> realFuture = (OperationFuture<Boolean>) source;
    final ComposablePromise<Boolean> res = ComposableFutures.newPromise();
    realFuture.addListener(new OperationCompletionListener() {
      @Override
      public void onComplete(OperationFuture<?> future) throws Exception {
        try {
          Boolean value = source.get();
          res.set(value);
        } catch (InterruptedException e) {
          res.setException(e);
        } catch (ExecutionException e) {
          res.setException(e.getCause() == null ? e : e.getCause());
        } catch (Exception e) {
          res.setException(e);
        }
      }
    });

    return res;
  }

}
