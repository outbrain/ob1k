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

      System.out.println("successes: " + success);
      System.out.println("failures: " + failure);
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

    System.out.println("final res: " + finalRes);
    System.out.println("successful updates: " + successfulUpdates);
    Assert.assertEquals(finalRes, successfulUpdates);
  }

}
