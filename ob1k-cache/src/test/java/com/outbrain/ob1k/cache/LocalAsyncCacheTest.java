package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.cache.CacheLoaderForTesting.*;
import static org.junit.Assert.*;

/**
 * Created by aronen on 4/22/14.
 * <p/>
 * test local cache behavior
 */
@SuppressWarnings("unchecked")
public class LocalAsyncCacheTest {

  private LocalAsyncCache<String, String> createCache() {
    return new LocalAsyncCache<>(createCacheConfiguration());
  }

  @NotNull
  private CacheConfiguration<String, String> createCacheConfiguration() {
    return new CacheConfiguration<String, String>("local").withMaxSize(3).withTtl(100).withLoader(new CacheLoaderForTesting());
  }

  @Test
  public void testLocalCache() {
    final LocalAsyncCache<String, String> cache = createCache();
    final String value1 = cache.getAsync("key1").getUnchecked();
    assertEquals(value1, VALUE_FOR + "key1");
    final Map<String, String> values = cache.getBulkAsync(Arrays.asList("key3", "key4")).getUnchecked();
    assertEquals(values.get("key3"), VALUE_FOR_BULK + "key3");
    assertEquals(values.get("key4"), VALUE_FOR_BULK + "key4");
    assertNull(values.get("key1"));
  }

  @Test
  public void testLocalCacheWithoutLoader() {
    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(new CacheConfiguration<String, String>("local").withMaxSize(3).withTtl(100));
    cache.setAsync("key1", "value1");
    final String value1 = cache.getAsync("key1").getUnchecked();
    assertEquals(value1, "value1");
    final String value2 = cache.getAsync("key2").getUnchecked();
    assertNull(value2);
    final Map<String, String> values = cache.getBulkAsync(Arrays.asList("key1", "key2")).getUnchecked();
    assertEquals(values.get("key1"), "value1");
    assertNull(values.get("key2"));
  }

  @Test
  public void testMissingKey() {
    final LocalAsyncCache<String, String> cache = createCache();
    final ComposableFuture<String> missingValue = cache.getAsync(MISSING_KEY);
    assertComposableFutureError(ERROR_MESSAGE, RuntimeException.class, missingValue);
  }

  @Test
  public void testMissingKeys() {
    final LocalAsyncCache<String, String> cache = createCache();
    final Map<String, String> values = cache.getBulkAsync(Arrays.asList("key1", MISSING_KEY)).getUnchecked();
    assertEquals(values.size(), 1);
    assertEquals(values.get("key1"), VALUE_FOR_BULK + "key1");
  }

  @Test
  public void testFailOnMissingKeys() {
    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(createCacheConfiguration().failOnMissingEntries(true));
    assertComposableFutureError(MISSING_KEY + " is missing from local loader response.", RuntimeException.class, cache.getBulkAsync(Arrays.asList("key1", MISSING_KEY)));
  }

  @Test
  public void testLoaderError() {
    final LocalAsyncCache<String, String> cache = createCache();
    assertComposableFutureError(TIMEOUT_MESSAGE, RuntimeException.class, cache.getBulkAsync(Arrays.asList("newKey", TIMEOUT_KEY)));
  }

  @Test
  public void testBadLoader() {
    final CacheLoader<String, String> cacheLoader = new CacheLoader<String, String>() {
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
    };

    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(new CacheConfiguration<String, String>("local").withLoader(cacheLoader).withMaxSize(3).withTtl(100));

    assertComposableFutureError("ooops", RuntimeException.class, cache.getAsync("error"));
    assertComposableFutureError(null, NullPointerException.class, cache.getAsync("null"));
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
      for (int i = 0; i < iterations; i++) {
        try {
          final boolean res = cache.setAsync(key, (key, value) -> new Box(value.number + 1), 100).get();

          if (res) {
            success++;
          } else {
            failure++;
          }

        } catch (final Exception ignore) {

        }

      }
      successCounter = success;
    }

    public int getSuccessCounter() {
      return successCounter;
    }

  }

  @Test
  public void testCacheWithMapper() throws Exception {
    final TypedCache<String, Box> cache = new LocalAsyncCache<>(new CacheConfiguration<String, Box>("cacheWithMapper").withMaxSize(300).withTtl(100, TimeUnit.SECONDS));
    final String cacheKey = "box";
    cache.setAsync(cacheKey, new Box(0)).get();

    final BoxUpdater updater1 = new BoxUpdater(cache, cacheKey, 100);
    final BoxUpdater updater2 = new BoxUpdater(cache, cacheKey, 100);
    final BoxUpdater updater3 = new BoxUpdater(cache, cacheKey, 100);
    final BoxUpdater updater4 = new BoxUpdater(cache, cacheKey, 100);

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
    final CacheLoaderForTesting loader = new CacheLoaderForTesting();
    loader.setGenerateLoaderErrors(true);
    final LocalAsyncCache<String, String> myCache = new LocalAsyncCache<>(new CacheConfiguration<String, String>("MyCache").withLoader(loader).withMaxSize(10).withTtl(10, TimeUnit.MINUTES));
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getAsync("a"));
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getAsync("b"));
    loader.setGenerateLoaderErrors(false);
    assertEquals(VALUE_FOR + "a", myCache.getAsync("a").get());
    assertEquals(VALUE_FOR + "b", myCache.getAsync("b").get());
    loader.setGenerateLoaderErrors(true); // already populated
    assertEquals(VALUE_FOR + "a", myCache.getAsync("a").get());
    assertEquals(VALUE_FOR + "b", myCache.getAsync("b").get());
  }


  @Test
  public void testCacheExceptionRemovalBulk() throws Exception {
    final CacheLoaderForTesting loader = new CacheLoaderForTesting();
    final LocalAsyncCache<String, String> myCache = new LocalAsyncCache<>(new CacheConfiguration<String, String>("MyCache").withLoader(loader).withMaxSize(10).withTtl(10, TimeUnit.MINUTES));

    loader.setGenerateLoaderErrors(true);
    final Iterable keys = Arrays.asList("a", "b", "null");
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getBulkAsync(keys));

    loader.setGenerateLoaderErrors(false);
    assertEquals(VALUE_FOR + "a", myCache.getAsync("a").get());
    final Map<String, String> bulk = (Map<String, String>) myCache.getBulkAsync(keys).get();
    assertEquals(VALUE_FOR + "a", bulk.get("a"));
    assertEquals(VALUE_FOR_BULK + "b", bulk.get("b"));
    assertNull(bulk.get(NULL_KEY));

    loader.setGenerateLoaderErrors(true);
    assertEquals(VALUE_FOR + "a", myCache.getAsync("a").get()); // already populated
    assertEquals(VALUE_FOR_BULK + "b", myCache.getAsync("b").get()); // already populated
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getAsync(NULL_KEY)); // nulls are not cached on bulk
  }


  private static <T> void assertComposableFutureError(String expectedMsg, Class<? extends Exception> exceptionType, ComposableFuture<T> future) {
    try {
      future.getUnchecked();
      fail("Expected to get exception");
    } catch (final RuntimeException e) {
      if (exceptionType != null) assertEquals(exceptionType, e.getClass());
      if (expectedMsg != null) assertEquals(expectedMsg, e.getMessage());
    }
  }
}
