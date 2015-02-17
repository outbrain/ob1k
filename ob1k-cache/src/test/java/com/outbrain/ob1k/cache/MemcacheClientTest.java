package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import junit.framework.Assert;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 2/4/15.
 */
@Ignore
public class MemcacheClientTest {
  private static TypedCache<String, String> client;
  private static MemcachedClientIF spyClient;

  @BeforeClass
  public static void initialize() throws IOException {
    spyClient = new MemcachedClient(new InetSocketAddress("localhost", 11211));
    client = new MemcacheClient<>(spyClient, new CacheKeyTranslator<String>() {
      @Override
      public String translateKey(final String key) {
        return key;
      }
    }, 1, TimeUnit.MINUTES);
  }

  @AfterClass
  public static void shutdown() {
    spyClient.shutdown();
  }

  @Test
  public void testGet() throws IOException, ExecutionException, InterruptedException {
    final ComposableFuture<String> res = client.setAsync("key1", "value1").continueOnSuccess(new FutureSuccessHandler<Boolean, String>() {
      @Override
      public ComposableFuture<String> handle(final Boolean result) {
        return client.getAsync("key1");
      }
    });

    final String result = res.get();
    Assert.assertEquals(result, "value1");
  }

  @Test
  public void testBulkApi() throws ExecutionException, InterruptedException {
    final Map<String, String> entries = new HashMap<>();
    for (int i=0; i< 100; i++) {
      entries.put("bulkKey" + i, "value" + i);
    }

    final ComposableFuture<Map<String, String>> res = client.setBulkAsync(entries).continueOnSuccess(new FutureSuccessHandler<Map<String, Boolean>, Map<String, String>>() {
      @Override
      public ComposableFuture<Map<String, String>> handle(final Map<String, Boolean> result) {
        return client.getBulkAsync(entries.keySet());
      }
    });

    final Map<String, String> results = res.get();
    Assert.assertEquals(results.size(), 100);
    for (int i=0; i< 100; i++) {
      Assert.assertEquals(results.get("bulkKey" + i), "value" + i);
    }

  }
}
