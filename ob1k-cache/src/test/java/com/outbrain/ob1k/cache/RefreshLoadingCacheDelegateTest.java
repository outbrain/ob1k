package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.cache.CacheLoaderForTesting.MISSING_KEY;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.NULL_KEY;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.VALUE_FOR;
import static com.outbrain.ob1k.cache.CacheLoaderForTesting.VALUE_FOR_BULK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RefreshLoadingCacheDelegateTest {

  private TypedCache<String, ValueWithWriteTime<String>> cacheMock = new TypedCacheMock();

  private CacheLoader<String, String> cacheLoaderStub = new CacheLoaderForTesting();

  private RefreshLoadingCacheDelegate<String, String> refreshingCache;

  @Before
  public void setup() {
    refreshingCache = new RefreshLoadingCacheDelegate<>(cacheMock, cacheLoaderStub, "myCache", null, 10, TimeUnit.SECONDS, false, 50, TimeUnit.MILLISECONDS, -1, null);
  }

  @Test
  public void testGetAsyncFromLoader() throws ExecutionException, InterruptedException {
    assertEquals(VALUE_FOR + "key1", refreshingCache.getAsync("key1").get());
  }

  @Test
  public void testGetAsyncFromLoaderNull() throws ExecutionException, InterruptedException {
    assertNull(refreshingCache.getAsync(NULL_KEY).get());
  }

  @Test
  public void testGetAsyncFromLoaderMissing() throws InterruptedException {
    try {
      assertNull(refreshingCache.getAsync(MISSING_KEY).get());
      fail();
    } catch (ExecutionException e) {
      // nothing
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetAsyncAndRefresh() throws ExecutionException, InterruptedException {
    CacheLoader<String, String> cacheLoaderMock = mock(CacheLoader.class);
    TypedCache<String, String> myCache = new RefreshLoadingCacheDelegate<>(cacheMock, cacheLoaderMock, "myCache", null, 10, TimeUnit.SECONDS, false, 500, TimeUnit.MILLISECONDS, -1, null);
    when(cacheLoaderMock.load(anyString(), anyString())).thenAnswer(invocation -> {
      Thread.sleep(100);
      String key = (String) invocation.getArguments()[1];
      return ComposableFutures.fromValue(VALUE_FOR + key);
    });
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(500);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    assertEquals(VALUE_FOR + "key1", myCache.getAsync("key1").get());
    verify(cacheLoaderMock, times(1)).load("myCache", "key1");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetAsyncAndRefreshNull() throws ExecutionException, InterruptedException {
    CacheLoader<String, String> cacheLoaderMock = mock(CacheLoader.class);
    TypedCache<String, String> myCache = new RefreshLoadingCacheDelegate<>(cacheMock, cacheLoaderMock, "myCache", null, 10, TimeUnit.SECONDS, false, 500, TimeUnit.MILLISECONDS, -1, null);
    when(cacheLoaderMock.load(anyString(), anyString())).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ComposableFutures.fromNull();
    });
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(500);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    assertNull(myCache.getAsync("key1").get());
    verify(cacheLoaderMock, times(1)).load("myCache", "key1");
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetAsyncAndRefreshException() throws ExecutionException, InterruptedException {
    CacheLoader<String, String> cacheLoaderMock = mock(CacheLoader.class);
    TypedCache<String, String> myCache = new RefreshLoadingCacheDelegate<>(cacheMock, cacheLoaderMock, "myCache", null, 10, TimeUnit.SECONDS, false, 5000, TimeUnit.MILLISECONDS, -1, null);
    when(cacheLoaderMock.load("myCache", "key1")).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ComposableFutures.fromError(new RuntimeException());
    });
    myCache.setAsync("key1", "value1").get();
    // get value before refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(5000);
    // get value and trigger refresh
    assertEquals("value1", myCache.getAsync("key1").get());
    Thread.sleep(200);
    //get value after refresh
    verify(cacheLoaderMock, times(1)).load("myCache", "key1");
    assertEquals("value1", myCache.getAsync("key1").get());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testGetBulkAsyncFromLoader() throws ExecutionException, InterruptedException {
    final Map<String, String> values = refreshingCache.getBulkAsync(Arrays.asList("key1", NULL_KEY, MISSING_KEY)).get();
    assertEquals(VALUE_FOR_BULK + "key1", values.get("key1"));
    assertNull(values.get(NULL_KEY));
    assertNull(values.get(MISSING_KEY));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void getBulkAsyncAndRefresh() throws ExecutionException, InterruptedException {
    CacheLoader<String, String> cacheLoaderMock = mock(CacheLoader.class);
    TypedCache<String, String> myCache = new RefreshLoadingCacheDelegate<>(cacheMock, cacheLoaderMock, "myCache", null, 10, TimeUnit.SECONDS, false, 500, TimeUnit.MILLISECONDS, -1, null);
    when(cacheLoaderMock.load(anyString(), any(Iterable.class))).thenAnswer(invocation -> {
      Thread.sleep(100);
      Iterable<String> keys = (Iterable<String>) invocation.getArguments()[1];
      Map<String, String> res = new HashMap<>();
      for (String key : keys) {
        res.put(key, VALUE_FOR_BULK + key);
      }
      return ComposableFutures.fromValue(res);
    });
    myCache.setAsync("key1", "value1").get();
    // get first value before refresh
    assertEquals("value1", myCache.getBulkAsync(Collections.singletonList("key1")).get().get("key1"));
    Thread.sleep(500);
    // get first value and trigger refresh, second value should not trigger refresh
    myCache.setAsync("key2", "value2").get();
    Map<String, String> res = myCache.getBulkAsync(Arrays.asList("key1", "key2")).get();
    assertEquals("value1", res.get("key1"));
    assertEquals("value2", res.get("key2"));
    Thread.sleep(200);
    //get value after refresh
    Map<String, String> res2 = myCache.getBulkAsync(Arrays.asList("key1", "key2")).get();
    assertEquals(VALUE_FOR_BULK + "key1", res2.get("key1"));
    assertEquals("value2", res2.get("key2"));
    verify(cacheLoaderMock, times(1)).load(eq("myCache"), eq(Collections.singletonList("key1")));
  }


  private static class TypedCacheMock implements TypedCache<String, ValueWithWriteTime<String>> {

    private Map<String, ValueWithWriteTime<String>> map = new HashMap<>();

    @Override
    public ComposableFuture<ValueWithWriteTime<String>> getAsync(final String key) {
      return ComposableFutures.fromValue(map.get(key));
    }

    @Override
    public ComposableFuture<Map<String, ValueWithWriteTime<String>>> getBulkAsync(final Iterable<? extends String> keys) {
      Map<String, ValueWithWriteTime<String>> res = new HashMap<>();
      for (String key : keys) {
        ValueWithWriteTime<String> value = map.get(key);
        if (value != null) {
          res.put(key, value);
        }
      }
      return ComposableFutures.fromValue(res);
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final String key, final ValueWithWriteTime<String> value) {
      map.put(key, value);
      return ComposableFutures.fromValue(true);
    }

    @Override
    public ComposableFuture<Boolean> setAsync(final String key, final EntryMapper<String, ValueWithWriteTime<String>> mapper, final int maxIterations) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ComposableFuture<Map<String, Boolean>> setBulkAsync(final Map<? extends String, ? extends ValueWithWriteTime<String>> entries) {
      Map<String, Boolean> res = new HashMap<>();
      entries.forEach((key, value) -> {
        map.put(key, value);
        res.put(key, true);
      });
      return ComposableFutures.fromValue(res);
    }

    @Override
    public ComposableFuture<Boolean> setIfAbsentAsync(final String key, final ValueWithWriteTime<String> value) {
      return ComposableFutures.fromValue(map.putIfAbsent(key, value) == null);
    }

    @Override
    public ComposableFuture<Boolean> deleteAsync(final String key) {
      return ComposableFutures.fromValue(map.remove(key) != null);
    }
  }
}
