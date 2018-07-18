package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.thimbleware.jmemcached.CacheImpl;
import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base test class for memcached client wrappers
 * @author Eran Harel
 */
public abstract class AbstractMemcachedClientTest {

  protected static final int MEMCACHED_PORT = 11311;
  private static MemCacheDaemon<LocalCacheElement> cacheDaemon;

  private TypedCache<String, Serializable> client;

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
    cacheDaemon.setBinary(false); // NOTE: JMemcached seem to have binary protocol bugs...
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

  protected abstract TypedCache<String, Serializable> createCacheClient() throws Exception;

  @Test
  public void testGetHit() throws IOException, ExecutionException, InterruptedException {
    final String key = "key1";
    final String expectedValue = "value1";
    final ComposableFuture<Serializable> res = client.setAsync(key, expectedValue)
      .flatMap(result -> client.getAsync(key));

    final Serializable result = res.get();
    assertEquals("unexpected result returned from getAsync()", expectedValue, result);
  }

  @Test
  public void testGetMiss() throws IOException, ExecutionException, InterruptedException {
    final ComposableFuture<Serializable> res = client.getAsync("keyMiss1");
    final Serializable result = res.get();
    assertNull("getAsync for unset key should have returned null", result);
  }

  @Test
  public void testGetBulkHit() throws ExecutionException, InterruptedException {
    final Map<String, String> expected = new HashMap<>();
    for (int i=0; i< 100; i++) {
      expected.put("bulkKey" + i, "value" + i);
    }

    final ComposableFuture<Map<String, Serializable>> res = client.setBulkAsync(expected)
      .flatMap(result -> client.getBulkAsync(expected.keySet()));

    final Map<String, Serializable> getResults = res.get();
    assertEquals("unexpected result returned from getBulkAsync()", expected, getResults);
  }

  @Test
  public void testGetBulkMiss() throws ExecutionException, InterruptedException {
    final Map<String, String> entries = new HashMap<>();
    for (int i=0; i< 100; i++) {
      entries.put("bulkKeyMiss" + i, "value" + i);
    }

    final ComposableFuture<Map<String, Serializable>> res = client.getBulkAsync(entries.keySet());
    final Map<String, Serializable> results = res.get();
    assertTrue("getBulkAsync for unset keys should have returned empty map", results.isEmpty());
  }

  @Test
  public void testDelete() throws IOException, ExecutionException, InterruptedException {
    final String key = "key1";
    final String expectedValue = "value1";
    final ComposableFuture<Boolean> res = client.setAsync(key, expectedValue)
      .flatMap(result -> client.deleteAsync(key));

    final Boolean result = res.get();
    assertTrue("unexpected result returned from getAsync()", result);
    assertNull("value was not deleted", client.getAsync(key).get());
  }


  @Test
  public void testCas() throws ExecutionException, InterruptedException {
    final String counterKey = "counterKey";
    client.deleteAsync(counterKey).get();

    final int iterations = 1000;
    final int threadCount = 2;
    final int successCount = runMultiThreadedCas(counterKey, iterations, threadCount);

    final int expectedSetCount = iterations * threadCount;
    assertEquals("Successful sets", expectedSetCount, successCount);
    assertEquals(expectedSetCount, client.getAsync(counterKey).get());
  }

  private int runMultiThreadedCas(final String counterKey, final int iterations, final int threadCount) throws InterruptedException {
    final AtomicInteger successCount = new AtomicInteger(0);
    final Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      threads[i] = new Thread(() -> {
        for (int t = 0; t < iterations; t++) {
          try {
            final Boolean res = client.setAsync(counterKey, (key, value) -> value == null ? 1 : (Integer)value + 1, iterations).get();
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

  @Test
  public void testSetIfAbsentAsync() throws ExecutionException, InterruptedException {
    final String key = UUID.randomUUID().toString();

    assertTrue(client.setIfAbsentAsync(key, "value").get());
    assertFalse(client.setIfAbsentAsync(key, "value").get());
  }

}
