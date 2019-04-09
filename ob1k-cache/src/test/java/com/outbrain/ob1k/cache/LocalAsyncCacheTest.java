package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.cache.CacheLoaderForTesting.ERROR_MESSAGE;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.MISSING_KEY;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.NULL_KEY;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.TEMPORARY_ERROR_MESSAGE;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.TIMEOUT_KEY;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.TIMEOUT_MESSAGE;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.VALUE_FOR;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.VALUE_FOR_BULK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aronen on 4/22/14.
 * <p/>
 * test local cache behavior
 */
@SuppressWarnings("unchecked")
public class LocalAsyncCacheTest {

  private static LocalAsyncCache<String, String> createCache() {
    return new LocalAsyncCache<>(3, 100, TimeUnit.MILLISECONDS, new CacheLoaderForTesting());
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
      assertNull(values.get("key1"));

    } catch (final Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testLocalCacheWithoutLoader() {
    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(3, 100, TimeUnit.MILLISECONDS);
    try {
      cache.setAsync("key1", "value1");
      final String value1 = cache.getAsync("key1").get();
      assertEquals(value1, "value1");
      final String value2 = cache.getAsync("key2").get();
      assertNull(value2);
      final Map<String, String> values = cache.<String>getBulkAsync(Arrays.asList("key1", "key2")).get();
      assertEquals(values.get("key1"),  "value1");
      assertNull(values.get("key2"));

    } catch (final Exception e) {
      fail(e.getMessage());
    }
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
  public void testCacheWithMapper() throws Exception {
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
    final CacheLoaderForTesting loader = new CacheLoaderForTesting() ;
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
    final CacheLoaderForTesting loader = new CacheLoaderForTesting() ;
    final LocalAsyncCache<String, String> myCache = new LocalAsyncCache<String, String>(10, 10, TimeUnit.MINUTES, loader, null, "MyCache");

    loader.setGenerateLoaderErrors(true);
    final Iterable keys = Arrays.asList("a","b","null");
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getBulkAsync(keys));

    loader.setGenerateLoaderErrors(false);
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get());
    final Map<String,String> bulk = (Map<String,String>) myCache.getBulkAsync(keys).get();
    assertEquals(VALUE_FOR+"a", bulk.get("a"));
    assertEquals(VALUE_FOR_BULK+"b", bulk.get("b"));
    assertNull(bulk.get(NULL_KEY));

    loader.setGenerateLoaderErrors(true);
    assertEquals(VALUE_FOR+"a", myCache.getAsync("a").get()); // already populated
    assertEquals(VALUE_FOR_BULK+"b", myCache.getAsync("b").get()); // already populated
    assertComposableFutureError(TEMPORARY_ERROR_MESSAGE, RuntimeException.class, myCache.getAsync(NULL_KEY)); // nulls are not cached on bulk
  }

  @Test
  public void testRefreshAfterWrite() throws ExecutionException, InterruptedException {
    final CacheLoader<String, String> loader = mock(CacheLoader.class);
    when(loader.load(anyString(), anyString())).thenAnswer(invocation -> ComposableFutures.submit(() -> {
      Thread.sleep(100);
      String key = (String) invocation.getArguments()[1];
      return VALUE_FOR + key;
    }));

    final LocalAsyncCache<String, String> myCache =
            new LocalAsyncCache.Builder<String, String>(10, 200000, TimeUnit.MILLISECONDS, null, "myCache")
                    .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
                    .withLoader(loader)
                    .build();
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(500);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    assertEquals(VALUE_FOR + "key1", myCache.getAsync("key1").get());
    verify(loader, times(1)).load("myCache", "key1");
  }

  @Test
  public void testGetAsyncAndRefreshNull() throws ExecutionException, InterruptedException {
    final CacheLoader<String, String> loader = mock(CacheLoader.class);
    when(loader.load(anyString(), anyString())).thenAnswer(invocation -> ComposableFutures.submit(() -> {
      Thread.sleep(100);
      return null;
    }));
    final LocalAsyncCache<String, String> myCache =
            new LocalAsyncCache.Builder<String, String>(10, 200000, TimeUnit.MILLISECONDS, null, "myCache")
                    .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
                    .withLoader(loader)
                    .build();
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(500);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    assertNull(myCache.getAsync("key1").get());
    verify(loader, times(1)).load("myCache", "key1");
  }

  @Test
  public void testGetAsyncAndRefreshException() throws ExecutionException, InterruptedException {
    final CacheLoader<String, String> loader = mock(CacheLoader.class);
    when(loader.load(anyString(), anyString())).thenAnswer(invocation -> ComposableFutures.submit(() -> {
      Thread.sleep(100);
      throw new RuntimeException();
    }));
    final LocalAsyncCache<String, String> myCache =
            new LocalAsyncCache.Builder<String, String>(10, 200000, TimeUnit.MILLISECONDS, null, "myCache")
                    .refreshAfterWrite(1000, TimeUnit.MILLISECONDS)
                    .withLoader(loader)
                    .build();
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(1000);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    verify(loader, times(2)).load("myCache", "key1");
  }

  @Test
  public void getBulkAsyncAndRefresh() throws ExecutionException, InterruptedException {
    CacheLoader<String, String> loader = mock(CacheLoader.class);
    final LocalAsyncCache<String, String> myCache =
            new LocalAsyncCache.Builder<String, String>(10, 200000, TimeUnit.MILLISECONDS, null, "myCache")
                    .refreshAfterWrite(500, TimeUnit.MILLISECONDS)
                    .withLoader(loader)
                    .build();
    when(loader.load(anyString(), anyString())).thenAnswer(invocation -> ComposableFutures.submit(() -> {
      Thread.sleep(100);
      String key = (String) invocation.getArguments()[1];
      return VALUE_FOR + key;
    }));
    myCache.setAsync("key1", "value1").get();
    // get first value before refresh
    assertEquals("value1", myCache.getBulkAsync(Arrays.asList("key1")).get().get("key1"));
    Thread.sleep(500);
    // get first value and trigger refresh, second value should not trigger refresh
    myCache.setAsync("key2", "value2").get();
    Map<String, String> res = myCache.getBulkAsync(Arrays.asList("key1", "key2")).get();
    assertEquals("value1", res.get("key1"));
    assertEquals("value2", res.get("key2"));
    Thread.sleep(200);
    //get value after refresh
    Map<String, String> res2 = myCache.getBulkAsync(Arrays.asList("key1", "key2")).get();
    assertEquals(VALUE_FOR + "key1", res2.get("key1"));
    assertEquals("value2", res2.get("key2"));
    verify(loader, times(1)).load("myCache", "key1");
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
