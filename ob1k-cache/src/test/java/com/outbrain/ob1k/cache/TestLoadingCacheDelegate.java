package com.outbrain.ob1k.cache;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.codahale3.CodahaleMetricsFactory;
import com.spotify.folsom.BinaryMemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
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
  public void testPartialLoading() throws ExecutionException, InterruptedException {
    final AtomicInteger loaderCounter = new AtomicInteger();
    final TypedCache<String, String> cache = new LocalAsyncCache<>();
    final TypedCache<String, String> loadingCache = new LoadingCacheDelegate<>(cache, new CacheLoader<String, String>() {
      ThreadLocalRandom random = ThreadLocalRandom.current();

      @Override
      public ComposableFuture<String> load(final String cacheName, final String key) {
        System.out.println("loading(single): " + key);
        loaderCounter.incrementAndGet();
        return ComposableFutures.schedule(() -> "res-" + key, random.nextLong(1, 19), TimeUnit.MILLISECONDS);
      }

      @Override
      public ComposableFuture<Map<String, String>> load(final String cacheName, final Iterable<? extends String> keys) {
        final Map<String, ComposableFuture<String>> res = new HashMap<>();
        for (final String key: keys) {
          System.out.println("loading(multiple): " + key);
          loaderCounter.incrementAndGet();
          res.put(key, ComposableFutures.schedule(() -> "res-" + key, random.nextLong(1, 19), TimeUnit.MILLISECONDS));
        }

        System.out.println("returning keys: " + res.keySet());
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

      System.out.println("---------------------------");
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  //////////////////////////////////////////////////////////////////////////////////////////////////////
  private MemcachedClientIF createSpyClient() {
    try {
      return new MemcachedClient(new ConnectionFactoryBuilder().setOpTimeout(100).build(), Lists.newArrayList(new InetSocketAddress("localhost", 11211)));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }


    private MemcachedClientIF spyClient = createSpyClient();
  private TypedCache<String, String> cacheClient = new MemcacheClient<>(spyClient, (CacheKeyTranslator<String>) key -> key, 100, TimeUnit.MILLISECONDS);;
//  private BinaryMemcacheClient<String> folsomClient = MemcacheClientBuilder.newStringClient().withRequestTimeoutMillis(100).withAddress(HostAndPort.fromParts("localhost", 11211)).connectBinary();
//  private TypedCache<String, String> cacheClient = new com.outbrain.ob1k.cache.memcache.folsom.MemcachedClient<>(folsomClient, (CacheKeyTranslator<String>) key -> key, 100l, TimeUnit.MILLISECONDS);

  private final TypedCache<String, String> LCD = new LoadingCacheDelegate<>(cacheClient, SLOW_CACHE_LOADER, "meh", metricFactory, 1, TimeUnit.SECONDS);

  @Test
  public void testTimeoutHang() throws Exception {
    final int NUM_THREADS = 128;
    for (int i = 0; i < NUM_THREADS; i++) {
      final Thread thread = new Thread(this::lotsOfGets, "lotsOfGets-" + i);
      thread.setDaemon(true);
      thread.start();
    }
//    lotsOfGets();

    Thread.sleep(100000);
  }

  public void lotsOfGets() {
    for (int i = 0; i < 10000000; i++) {
      if (i % 10000 == 0) {
        System.out.println(i);
        try {
          Thread.sleep(10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      LCD.getAsync("key").consume(result -> {
        if (!result.isSuccess())
          System.err.println("QQQQQQQQQQQ" + result.getError());
      });
    }
  }
}
