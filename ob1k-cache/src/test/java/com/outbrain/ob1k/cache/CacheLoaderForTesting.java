package com.outbrain.ob1k.cache;

import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CacheLoaderForTesting implements CacheLoader<String, String> {
  public static final String VALUE_FOR = "ValueFor-";
  public static final String VALUE_FOR_BULK = "ValueFor-Bulk-";
  public static final String MISSING_KEY = "missing-key";
  public static final String NULL_KEY = "null-key";
  public static final String ERROR_MESSAGE = "missing key";
  public static final String TIMEOUT_KEY = "timeOutKey";
  public static final String TIMEOUT_MESSAGE = "timeout occurred";
  public static final String TEMPORARY_ERROR_MESSAGE = "Load failed temporarily";

  private final AtomicBoolean generateLoaderErrors = new AtomicBoolean(false);

  public void setGenerateLoaderErrors(final boolean val) {
    generateLoaderErrors.set(val);
  }

  @Override
  public ComposableFuture<String> load(final String cacheName, final String key) {
    if (generateLoaderErrors.get()) {
      return ComposableFutures.fromError(new RuntimeException(TEMPORARY_ERROR_MESSAGE));
    }
    if (key.equals(MISSING_KEY)) {
      return ComposableFutures.fromError(new RuntimeException(ERROR_MESSAGE));
    }
    if (key.equals(NULL_KEY)) {
      return ComposableFutures.fromNull();
    }
    return ComposableFutures.fromValue(VALUE_FOR + key);
  }

  @Override
  public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
    if (generateLoaderErrors.get()) {
      return ComposableFutures.fromError(new RuntimeException(TEMPORARY_ERROR_MESSAGE));
    }
    if (Iterables.contains(keys, TIMEOUT_KEY)) {
      return ComposableFutures.fromError(new RuntimeException(TIMEOUT_MESSAGE));
    }
    final HashMap<String, String> res = new HashMap<>();
    for (String key : keys) {
      if (key.equals(NULL_KEY)) {
        res.put(key, null);
      }
      else if (!key.equals(MISSING_KEY)) {
        res.put(key, VALUE_FOR_BULK + key);
      }
    }
    return ComposableFutures.fromValue(res);
  }
}
