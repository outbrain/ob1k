package com.outbrain.ob1k.cache.memcache.folsom;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.outbrain.ob1k.cache.AbstractMemcachedClientTest;
import com.outbrain.ob1k.cache.TypedCache;
import com.spotify.folsom.AsciiMemcacheClient;
import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import com.spotify.folsom.Transcoder;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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


  @Test(expected = ExecutionException.class)
  public void testMultiget_TranscoderExecption() throws ExecutionException, InterruptedException, TimeoutException {
    Transcoder<Serializable> transcoder = new Transcoder<Serializable>() {
      @Override
      public Serializable decode(byte[] b) {
        throw new SerializationException("QQQQQ YYYYY");
      }

      @Override
      public byte[] encode(Serializable t) {
        return SerializationUtils.serialize(t);
      }
    };

    final AsciiMemcacheClient<Serializable> rawClient = new MemcacheClientBuilder<Serializable>(transcoder).withAddress(HostAndPort.fromParts("localhost", MEMCACHED_PORT))
      .withRequestTimeoutMillis(1000)
      .connectAscii();
    ConnectFuture.connectFuture(rawClient).get();
    final MemcachedClient<Object, Serializable> client = new MemcachedClient<>(rawClient, k -> "K", 1, TimeUnit.MINUTES);

    client.setAsync("meh", "its here").get();
    client.getBulkAsync(Lists.newArrayList("meh", "bah")).get(1, TimeUnit.MINUTES);
  }
}
