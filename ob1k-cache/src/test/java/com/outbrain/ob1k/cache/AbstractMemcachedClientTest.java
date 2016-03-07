package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Base test class for memcached client wrappers
 * @author Eran Harel
 */
public abstract class AbstractMemcachedClientTest {

  public static final int MEMCACHED_PORT = 11311;
  private static MemcachedClientIF spyClient;
  private static MemCacheDaemon<LocalCacheElement> cacheDaemon;

  private TypedCache<String, String> client;

  @BeforeClass
  public static void setupBeforeClass() throws IOException {
    createCacheDaemon();
    spyClient = new MemcachedClient(new InetSocketAddress("localhost", MEMCACHED_PORT));
  }

  private static void createCacheDaemon() {
    cacheDaemon = new MemCacheDaemon<>();
    cacheDaemon.setCache(new CacheImpl(ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1000, 4194304)));
    cacheDaemon.setAddr(new InetSocketAddress(MEMCACHED_PORT));
    cacheDaemon.setVerbose(false);
    cacheDaemon.start();
  }

  @AfterClass
  public static void teardownAfterClass() {
    spyClient.shutdown();
    cacheDaemon.stop();
  }

  @Before
  public void setup() throws Exception {
    client = createCacheClient();
  }

  protected abstract TypedCache<String, String> createCacheClient() throws Exception;

  public static MemcachedClientIF getSpyClient() {
    return spyClient;
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
