package com.outbrain.ob1k.cache;

import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 4/22/14.
 * <p/>
 * test local cache behavior
 */
public class LocalAsyncCacheTest {

  private static final String VALUE_FOR = "ValueFor-";
  private static final String MISSING_KEY = "missing-key";
  public static final String ERROR_MESSAGE = "missing key";
  public static final String TIMEOUT_KEY = "timeOutKey";
  public static final String TIMEOUT_MESSAGE = "timeout occurred";

  private static LocalAsyncCache<String, String> createCache() {
    return new LocalAsyncCache<>(3, 100, TimeUnit.MILLISECONDS, new CacheLoader<String, String>() {

      @Override
      public ComposableFuture<String> load(final String cacheName, final String key) {
        if (key.equals(MISSING_KEY)) {
          return ComposableFutures.fromError(new RuntimeException(ERROR_MESSAGE));
        }

        return ComposableFutures.fromValue(VALUE_FOR + key);
      }

      @Override
      public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
        if (Iterables.contains(keys, TIMEOUT_KEY)) {
          System.out.println("returning a timeout error");
          return ComposableFutures.fromError(new RuntimeException(TIMEOUT_MESSAGE));
        }

        final Map<String, String> results = new HashMap<>();
        for (final String key : keys) {
          if (!key.equals(MISSING_KEY)) {
            results.put(key, VALUE_FOR + key);
          }
        }

        return ComposableFutures.fromValue(results);
      }
    });
  }


  @Test
  public void testLocalCache() {
    final LocalAsyncCache<String, String> cache = createCache();
    try {
      final String value1 = cache.getAsync("key1").get();
      Assert.assertEquals(value1, VALUE_FOR + "key1");
      final Map<String, String> values = cache.<String>getBulkAsync(Arrays.asList("key3", "key4")).get();
      Assert.assertEquals(values.get("key3"), VALUE_FOR + "key3");
      Assert.assertEquals(values.get("key4"), VALUE_FOR + "key4");
      Assert.assertEquals(values.get("key1"), null);

    } catch (final Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testMissingKey() {
    final LocalAsyncCache<String, String> cache = createCache();
    final ComposableFuture<String> missingValue = cache.getAsync(MISSING_KEY);
    try {
      missingValue.get();
      Assert.fail("should return an error.");
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    } catch (final ExecutionException e) {
      Assert.assertEquals(e.getCause().getMessage(), ERROR_MESSAGE);
    }
  }

  @Test
  public void testMissingKeys() {
    final LocalAsyncCache<String, String> cache = createCache();

    try {
      final Map<String, String> values = cache.getBulkAsync(Arrays.asList("key1", MISSING_KEY)).get();
      Assert.assertEquals(values.size(), 1);
      Assert.assertEquals(values.get("key1"), VALUE_FOR + "key1");
    } catch (InterruptedException | ExecutionException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testLoaderError() {
    final LocalAsyncCache<String, String> cache = createCache();

    try {
      final Map<String, String> partialValues = cache.getBulkAsync(Arrays.asList("newKey", TIMEOUT_KEY)).get();
      Assert.fail("should fail with timeout. " + partialValues.size());
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause().getMessage().contains(TIMEOUT_MESSAGE));
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    }
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

    try {
      final ComposableFuture<String> futureRes = cache.getAsync("error");
      futureRes.get();
      Assert.fail("should get an exception");
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    } catch (final ExecutionException e) {
      Assert.assertEquals(e.getCause().getMessage(), "ooops");
    }

    try {
      cache.getAsync("null").get();
      Assert.fail("should get an exception");
    } catch (final InterruptedException e) {
      Assert.fail(e.getMessage());
    } catch (final ExecutionException e) {
      Assert.assertTrue(e.getCause() instanceof NullPointerException);
    }
  }

  @Test
  public void testCAS() throws Exception {
    final LocalAsyncCache<String, String> cache = new LocalAsyncCache<>(300, 100, TimeUnit.SECONDS);
    final Boolean res1 = cache.setAsync("1", "first").get(); // unconditional set.
    final Boolean res2 = cache.setAsync("1", "first", "second").get();
    final Boolean res3 = cache.setAsync("1", "first", "third").get();

    Assert.assertEquals(res1, Boolean.TRUE);
    Assert.assertEquals(res2, Boolean.TRUE);
    Assert.assertEquals(res3, Boolean.FALSE);
  }

}
