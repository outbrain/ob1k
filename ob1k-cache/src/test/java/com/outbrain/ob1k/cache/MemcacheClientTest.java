package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;
import com.outbrain.ob1k.cache.metrics.MonitoringCacheDelegate;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * @author aronen
 */
public class MemcacheClientTest {
  public static final int MEMCACHED_PORT = 11311;
  private static TypedCache<String, String> client;

  private static MemcachedClientIF spyClient;
  private static MemCacheDaemon<LocalCacheElement> cacheDaemon;

  @BeforeClass
  public static void initialize() throws IOException {
    createCacheDaemon();

    spyClient = new MemcachedClient(new InetSocketAddress("localhost", MEMCACHED_PORT));
    client = new MemcacheClient<>(spyClient, (CacheKeyTranslator<String>) key -> key, 1, TimeUnit.MINUTES);

    final MetricFactory metricFactory = mock(MetricFactory.class, withSettings().defaultAnswer(RETURNS_MOCKS));
    client = new MonitoringCacheDelegate<>(client, MemcacheClientTest.class.getSimpleName(), metricFactory);
  }

  private static void createCacheDaemon() {
    cacheDaemon = new MemCacheDaemon<>();
    cacheDaemon.setCache(new CacheImpl(ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1000, 4194304)));
    cacheDaemon.setAddr(new InetSocketAddress(MEMCACHED_PORT));
    cacheDaemon.setVerbose(false);
    cacheDaemon.start();
  }

  @AfterClass
  public static void shutdown() {
    spyClient.shutdown();
    cacheDaemon.stop();
  }

  @Test
  public void testGetHit() throws IOException, ExecutionException, InterruptedException {
    final ComposableFuture<String> res = client.setAsync("key1", "value1")
      .continueOnSuccess((FutureSuccessHandler<Boolean, String>) result -> client.getAsync("key1"));

    final String result = res.get();
    Assert.assertEquals(result, "value1");
  }

  @Test
  public void testGetMiss() throws IOException, ExecutionException, InterruptedException {
    final ComposableFuture<String> res = client.getAsync("keyMiss1");
    final String result = res.get();
    Assert.assertNull(result);
  }

  @Test
  public void testGetBulkHit() throws ExecutionException, InterruptedException {
    final Map<String, String> entries = new HashMap<>();
    for (int i=0; i< 100; i++) {
      entries.put("bulkKey" + i, "value" + i);
    }

    final ComposableFuture<Map<String, String>> res = client.setBulkAsync(entries)
      .continueOnSuccess((FutureSuccessHandler<Map<String, Boolean>, Map<String, String>>) result -> client.getBulkAsync(entries.keySet()));

    final Map<String, String> results = res.get();
    Assert.assertEquals(results.size(), 100);
    for (int i=0; i< 100; i++) {
      Assert.assertEquals(results.get("bulkKey" + i), "value" + i);
    }
  }

  @Test
  public void testGetBulkMiss() throws ExecutionException, InterruptedException {
    final Map<String, String> entries = new HashMap<>();
    for (int i=0; i< 100; i++) {
      entries.put("bulkKeyMiss" + i, "value" + i);
    }

    final ComposableFuture<Map<String, String>> res = client.getBulkAsync(entries.keySet());
    final Map<String, String> results = res.get();
    Assert.assertEquals(results.size(), 0);
  }
}
