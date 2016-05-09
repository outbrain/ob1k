package com.outbrain.ob1k.cache;

import com.codahale.metrics.MetricRegistry;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.codahale3.CodahaleMetricsFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * emulate a loader that takes a while and test to se that values are loaded only once.
 *
 * @author aronen on 10/27/14.
 */
public class TestLoadingCacheDelegate {

  private final MetricRegistry registry = new MetricRegistry();
  private final MetricFactory metricFactory = new CodahaleMetricsFactory(registry);

  private static final CacheLoader<String, String> SLOW_CACHE_LOADER = new CacheLoader<String, String>() {

    @Override
    public ComposableFuture<String> load(final String cacheName, final String key) {
      return slowResponse(2);
    }

    @Override
    public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
      return null;
    }
  };

  private static final TypedCache<String, String> SLOW_CACHE = new LocalAsyncCache<String, String>() {
    @Override
    public ComposableFuture<String> getAsync(final String key) {
      return slowResponse(100);
    }
  };

  private static ComposableFuture<String> slowResponse(final int responseDuration) {
    return ComposableFutures.schedule(() -> "res", responseDuration, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testCacheTimeoutHandling() throws InterruptedException {
    final TypedCache<String, String> loadingCache = new LoadingCacheDelegate<>(SLOW_CACHE, SLOW_CACHE_LOADER, "meh", metricFactory, 1, TimeUnit.MILLISECONDS);

    try {
      loadingCache.getAsync("key").get();
    } catch (final ExecutionException e) {
      Assert.assertEquals("cache timeouts", 1, registry.getCounters().get("LoadingCacheDelegate.meh.cacheTimeouts").getCount());
    }
  }

  @Test
  public void testLoaderTimeoutHandling() throws InterruptedException {
    final TypedCache<String, String> cache = new LocalAsyncCache<>();
    final TypedCache<String, String> loadingCache = new LoadingCacheDelegate<>(cache, SLOW_CACHE_LOADER, "meh", metricFactory, 1, TimeUnit.MILLISECONDS);

    try {
      loadingCache.getAsync("key").get();
    } catch (final ExecutionException e) {
      Assert.assertEquals("cache timeouts", 1, registry.getCounters().get("LoadingCacheDelegate.meh.loaderTimeouts").getCount());
    }
  }

  @Test
  @SuppressWarnings("unchecked") final
  public void testCacheLoadResultsFailure_shouldCountErrors() throws ExecutionException, InterruptedException {
    final String key = "key";
    final String value = "value";
    final String cacheName = "meh";
    final TypedCache<String, String> mockCache = Mockito.mock(TypedCache.class);
    Mockito.when(mockCache.getAsync(key)).thenReturn(ComposableFutures.fromNull());
    Mockito.when(mockCache.setAsync(key, value)).thenReturn(ComposableFutures.fromError(new RuntimeException("MOCK failure")));

    final CacheLoader<String,String> mockLoader = Mockito.mock(CacheLoader.class);
    Mockito.when(mockLoader.load(cacheName, key)).thenReturn(ComposableFutures.fromValue(value));

    final TypedCache<String, String> loadingCache = new LoadingCacheDelegate<>(mockCache, mockLoader, cacheName, metricFactory, 1, TimeUnit.HOURS);
    loadingCache.getAsync(key).get();

    Assert.assertEquals("expected errros", 1, registry.getCounters().get("LoadingCacheDelegate.meh.cacheErrors").getCount());
  }

  @Test
  public void testPartialLoading() throws ExecutionException, InterruptedException {
    final AtomicInteger loaderCounter = new AtomicInteger();
    final TypedCache<String, String> cache = new LocalAsyncCache<>();
    final TypedCache<String, String> loadingCache = new LoadingCacheDelegate<>(cache, new CacheLoader<String, String>() {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      @Override
      public ComposableFuture<String> load(final String cacheName, final String key) {
        loaderCounter.incrementAndGet();
        return ComposableFutures.schedule(() -> "res-" + key, random.nextLong(1, 19), TimeUnit.MILLISECONDS);
      }

      @Override
      public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
        final Map<String, ComposableFuture<String>> res = new HashMap<>();
        for (final String key: keys) {
          loaderCounter.incrementAndGet();
          res.put(key, ComposableFutures.schedule(() -> "res-" + key, random.nextLong(1, 19), TimeUnit.MILLISECONDS));
        }

        return ComposableFutures.all(true, res);
      }
    }, "default");

    for (int i=0;i < 100; i++) {
      final ComposableFuture<String> res1 = loadingCache.getAsync("1");
      final ComposableFuture<Map<String, String>> res2 = loadingCache.getBulkAsync(Arrays.asList("1", "2"));
      final ComposableFuture<Map<String, String>> res3 = loadingCache.getBulkAsync(Arrays.asList("1", "2", "3", "4", "5"));

      Assert.assertEquals(res1.get(), "res-1");
      Assert.assertEquals(res2.get().size(), 2);
      Assert.assertEquals(res3.get().size(), 5);
      Assert.assertEquals(res3.get().get("5"), "res-5");

      Assert.assertTrue(loaderCounter.get() <= 5);

      loadingCache.deleteAsync("1").get();
      loadingCache.deleteAsync("2").get();
      loadingCache.deleteAsync("3").get();
      loadingCache.deleteAsync("4").get();
      loadingCache.deleteAsync("5").get();

//      Thread.sleep(200L);
      loaderCounter.set(0);
    }

  }

}
