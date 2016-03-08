package com.outbrain.ob1k.cache.memcache.folsom;

import com.google.common.net.HostAndPort;
import com.outbrain.ob1k.cache.AbstractMemcachedClientTest;
import com.outbrain.ob1k.cache.TypedCache;
import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author Eran Harel
 */
public class MemcachedClientTest extends AbstractMemcachedClientTest {

  private static MemcacheClient<Serializable> folsomClient;

  @BeforeClass
  public static void setupBeforeClass() throws Exception {
    folsomClient = MemcacheClientBuilder.newSerializableObjectClient()
      .withAddress(HostAndPort.fromParts("localhost", MEMCACHED_PORT))
      .withRequestTimeoutMillis(1000)
      .connectAscii();
    ConnectFuture.connectFuture(folsomClient).get();
  }

  @AfterClass
  public static void teardownAfterClass() {
    folsomClient.shutdown();
  }

  @Override
  protected TypedCache<String, Serializable> createCacheClient() throws Exception {
    return new MemcachedClient<>(folsomClient, key -> key, 1, TimeUnit.MINUTES);
  }
}
