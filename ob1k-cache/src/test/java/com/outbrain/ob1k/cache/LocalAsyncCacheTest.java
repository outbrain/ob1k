package com.outbrain.ob1k.cache;

import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.junit.Test;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/**
 * Created by aronen on 4/22/14.
 * <p/>
 * test local cache behavior
 */
public class LocalAsyncCacheTest {

  private static final String VALUE_FOR = "ValueFor-";
  private static final String VALUE_FOR_BULK = "ValueFor-Bulk-";
  private static final String MISSING_KEY = "missing-key";
  private static final String NULL_KEY = "null-key";
  public static final String ERROR_MESSAGE = "missing key";
  public static final String TIMEOUT_KEY = "timeOutKey";
  public static final String TIMEOUT_MESSAGE = "timeout occurred";
  public static final String TEMPORARY_ERROR_MESSAGE = "Load failed temporarily";

  private static class ExceptionalCacheLoader implements CacheLoader<String, String> {
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
      return ComposableFutures.fromValue(VALUE_FOR+key);
    }

    @Override
    public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
      if (generateLoaderErrors.get()) {
        return ComposableFutures.fromError(new RuntimeException(TEMPORARY_ERROR_MESSAGE));
      }
      if (Iterables.contains(keys, TIMEOUT_KEY)) {
        return ComposableFutures.fromError(new RuntimeException(TIMEOUT_MESSAGE));
      }
      final HashMap<String,String> res = new HashMap<>();
      for (String key:keys) {
        if (key.equals(NULL_KEY)) {
          res.put(key, null);
        } if (!key.equals(MISSING_KEY)) {
          res.put(key, VALUE_FOR_BULK + key);
        }
      }
      return ComposableFutures.fromValue(res);
    }
  }

  private static LocalAsyncCache<String, String> createCache() {
    return new LocalAsyncCache<>(3, 100, TimeUnit.MILLISECONDS, new ExceptionalCacheLoader());
  }

  @Test
  public void testLocalCache() {
    final LocalAsyncCache<String, String> cache = createCache();
    try {
      final String value1 = cache.getAsync("key1").get();
      assertEquals(value1, VALUE_FOR + "key1");
      final Map<String, String> values = cache.<String>getBulkAsync(Arrays.asList("key3", "key4")).get();
      assertEquals(values.get("key3"), VALUE_FOR_BULK + "key3");
      assertEquals(values.get("key4"), VALUE_FOR_BULK + "key4");
      assertEquals(values.get("key1"), null);

    } catch (final Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testMissingKey() {
    final LocalAsyncCache<String, String> cache = createCache();
    final ComposableFuture<String> missingValue = cache.getAsync(MISSING_KEY);
    assertComposableFutureError(ERROR_MESSAGE,RuntimeException.class,missingValue);
  }

  @Test
  public void testMissingKeys() {
    final LocalAsyncCache<String, String> cache = createCache();

    try {
      final Map<String, String> values = cache.getBulkAsync(Arrays.asList("key1", MISSING_KEY)).get();
      assertEquals(values.size(), 1);
      assertEquals(values.get("key1"), VALUE_FOR_BULK + "key1");
    } catch (InterruptedException | ExecutionException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testLoaderError() {
    final LocalAsyncCache<String, String> cache = createCache();
    assertComposableFutureError(TIMEOUT_MESSAGE,RuntimeException.class,cache.getBulkAsync(Arrays.asList("newKey", TIMEOUT_KEY)));
  }

  @Test
  public void testBadLoader() {
    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(3, 100, TimeUnit.MILLISECONDS, new CacheLoader<String, String>() {
      @Override
      public ComposableFuture<String> load(final String cacheName, final String key) {
        if (key.equals("error")) {
          throw new RuntimeException("ooops");
        } else if (key.equals("null")) {
          return null;
        } else {
          return ComposableFutures.fromValue("value");
        }
      }

      @Override
      public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
        throw new RuntimeException("ooops");
      }
    });

    assertComposableFutureError("ooops",RuntimeException.class, cache.getAsync("error"));
    assertComposableFutureError(null,NullPointerException.class, cache.getAsync("null"));
  }

  public static class Box {
    public final int number;

    public Box(final int number) {
      this.number = number;
    }
  }

  public static class BoxUpdater extends Thread {
    private final TypedCache<String, Box> cache;
    private final String key;
    private final int iterations;
    private volatile int successCounter;

    public BoxUpdater(final TypedCache<String, Box> cache, final String key, final int iterations) {
      this.cache = cache;
      this.key = key;
      this.iterations = iterations;
    }

    @Override
    public void run() {
      int success = 0;
      int failure = 0;
      for (int i =0; i < iterations; i++) {
        try {
          final boolean res = cache.setAsync(key, new EntryMapper<String, Box>() {
            @Override
            public Box map(final String key, final Box value) {
              return new Box(value.number + 1);
            }
          }, 100).get();

          if (res) {
            success++;
          } else {
            failure++;
          }

        } catch (final Exception e) {
          e.printStackTrace();
        }

      }
      successCounter = success;
    }

    public int getSuccessCounter() {
      return successCounter;
    }
  }

  @Test
  public void testCasWithMapper() throws Exception {
    final TypedCache<String, Box> cache = new LocalAsyncCache<>(300, 100, TimeUnit.SECONDS);
    final String cacheKey = "box";
    cache.setAsync(cacheKey, new Box(0));

    final BoxUpdater updater1 = new BoxUpdater(cache, cacheKey, 1000000);
    final BoxUpdater updater2 = new BoxUpdater(cache, cacheKey, 1000000);
    final BoxUpdater updater3 = new BoxUpdater(cache, cacheKey, 1000000);
    final BoxUpdater updater4 = new BoxUpdater(cache, cacheKey, 1000000);

    updater1.start();
    updater2.start();
    updater3.start();
    updater4.start();

    updater1.join();
    updater2.join();
    updater3.join();
    updater4.join();

    final int finalRes = cache.getAsync(cacheKey).get().number;
    final int successfulUpdates = updater1.getSuccessCounter() +
                                  updater2.getSuccessCounter() +
                                  updater3.getSuccessCounter() +
                                  updater4.getSuccessCounter();

    assertEquals(finalRes, successfulUpdates);
  }



  @Test
  public void testCacheExceptionRemoval() throws Exception {
    final ExceptionalCacheLoader loader = new ExceptionalCacheLoader() ;
    loader.setGenerateLoaderErrors(true);
    final LocalAsyncCache<String, String> myCache = new LocalAsyncCache<String, String>(10, 10, TimeUnit.MINUTES, loader, null, "MyCache");
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE,RuntimeException.class,myCache.getAsync("a"));
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE,RuntimeException.class,myCache.getAsync("b"));
    loader.setGenerateLoaderErrors(false);
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get());
    assertEquals(VALUE_FOR+"b", myCache.getAsync("b").get());
    loader.setGenerateLoaderErrors(true); // already populated
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get());
    assertEquals(VALUE_FOR+"b", myCache.getAsync("b").get());
  }


  @Test
  public void testCacheExceptionRemovalBulk() throws Exception {
    final ExceptionalCacheLoader loader = new ExceptionalCacheLoader() ;
    final LocalAsyncCache<String, String> myCache = new LocalAsyncCache<String, String>(10, 10, TimeUnit.MINUTES, loader, null, "MyCache");

    loader.setGenerateLoaderErrors(true);
    final Iterable keys = Arrays.asList("a","b","null");
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE,RuntimeException.class,myCache.getBulkAsync(keys));

    loader.setGenerateLoaderErrors(false);
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get());
    final Map<String,String> bulk = (Map<String,String>)myCache.getBulkAsync(keys).get();
    assertEquals(VALUE_FOR+"a",bulk.get("a"));
    assertEquals(VALUE_FOR_BULK+"b",bulk.get("b"));
    assertNull(bulk.get(NULL_KEY));

    loader.setGenerateLoaderErrors(true);
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get()); // already populated
    assertEquals(VALUE_FOR_BULK+"b", myCache.getAsync("b").get()); // already populated
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE,RuntimeException.class,myCache.getAsync(NULL_KEY)); // nulls are not cached on bulk
  }

  private static <T> void assertComposableFutureError(String expectedMsg, Class<? extends Exception> exceptionType, ComposableFuture<T> future) {
    try {
      future.get();
      fail("Expected to get exception");
    } catch (final InterruptedException e) {
      fail("Interrupted ?"+ e.getMessage());
    } catch (ExecutionException e) {
      if (exceptionType != null) assertEquals(exceptionType,e.getCause().getClass());
      if (expectedMsg != null) assertEquals(expectedMsg,e.getCause().getMessage());
    }
  }
}
