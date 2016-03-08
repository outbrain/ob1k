package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base test class for memcached client wrappers
 * @author Eran Harel
 */
public abstract class AbstractMemcachedClientTest {

  public static final int MEMCACHED_PORT = 11311;
  private static MemCacheDaemon<LocalCacheElement> cacheDaemon;

  private TypedCache<String, String> client;

  @BeforeClass
  public static void setupsBeforeClass_super() throws Exception {
    createCacheDaemon();
  }

  private static void createCacheDaemon() {
    if (cacheDaemon != null) {
      return;
    }
    cacheDaemon = new MemCacheDaemon<>();
    cacheDaemon.setCache(new CacheImpl(ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.FIFO, 1000, 4194304)));
    cacheDaemon.setAddr(new InetSocketAddress(MEMCACHED_PORT));
    cacheDaemon.setVerbose(false);
    cacheDaemon.start();
  }

  @AfterClass
  public static void teardownAfterClass() {
    cacheDaemon.stop();
  }

  @Before
  public void setup() throws Exception {
    client = createCacheClient();
  }

  protected abstract TypedCache<String, String> createCacheClient() throws Exception;

  @Test
  public void testGetHit() throws IOException, ExecutionException, InterruptedException {
    final String key = "key1";
    final String expectedValue = "value1";
    final ComposableFuture<String> res = client.setAsync(key, expectedValue)
      .continueOnSuccess((FutureSuccessHandler<Boolean, String>) result -> client.getAsync(key));

    final String result = res.get();
    Assert.assertEquals("unexpected result returned from getAsync()", expectedValue, result);
  }

  @Test
  public void testGetMiss() throws IOException, ExecutionException, InterruptedException {
    final ComposableFuture<String> res = client.getAsync("keyMiss1");
    final String result = res.get();
    Assert.assertNull("getAsync for unset key should have returned null", result);
  }

  @Test
  public void testGetBulkHit() throws ExecutionException, InterruptedException {
    final Map<String, String> expected = new HashMap<>();
    for (int i=0; i< 100; i++) {
      expected.put("bulkKey" + i, "value" + i);
    }

    final ComposableFuture<Map<String, String>> res = client.setBulkAsync(expected)
      .continueOnSuccess((FutureSuccessHandler<Map<String, Boolean>, Map<String, String>>) result -> client.getBulkAsync(expected.keySet()));

    final Map<String, String> getResults = res.get();
    Assert.assertEquals("unexpected result returned from getBulkAsync()", expected, getResults);
  }

  @Test
  public void testGetBulkMiss() throws ExecutionException, InterruptedException {
    final Map<String, String> entries = new HashMap<>();
    for (int i=0; i< 100; i++) {
      entries.put("bulkKeyMiss" + i, "value" + i);
    }

    final ComposableFuture<Map<String, String>> res = client.getBulkAsync(entries.keySet());
    final Map<String, String> results = res.get();
    Assert.assertTrue("getBulkAsync for unset keys should have returned empty map", results.isEmpty());
  }

  @Test
  public void testCas() throws ExecutionException, InterruptedException {
    final String counterKey = "counterKey";
    client.setAsync(counterKey, "0").get();

    final int iterations = 1000;
    final int threadCount = 4;
    final int successCount = runMultiThreadedCas(counterKey, iterations, threadCount);

    final int expectedSetCount = iterations * threadCount;
    Assert.assertEquals("Successful sets", expectedSetCount, successCount);
    Assert.assertEquals(String.valueOf(expectedSetCount), client.getAsync(counterKey).get());
  }

  private int runMultiThreadedCas(final String counterKey, final int iterations, final int threadCount) throws InterruptedException {
    final AtomicInteger successCount = new AtomicInteger(0);
    final Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new Thread(() -> {
        for (int t = 0; t < iterations; t++) {
          try {
            final Boolean res = client.setAsync(counterKey, (key, value) -> String.valueOf(Long.parseLong(value) + 1), 500).get();
            if (res) {
              successCount.incrementAndGet();
            }
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
        }
      });
      threads[i].start();
    }

    for (final Thread thread : threads) {
      thread.join();
    }
    return successCount.get();
  }

}
