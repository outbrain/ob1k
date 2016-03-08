package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;
import com.outbrain.ob1k.cache.metrics.MonitoringCacheDelegate;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.transcoders.WhalinTranscoder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * @author aronen
 */
public class MemcacheClientTest extends AbstractMemcachedClientTest {

  private static MemcachedClientIF spyClient;

  @BeforeClass
  public static void setupBeforeClass() throws IOException {
    final ConnectionFactory cf = new ConnectionFactoryBuilder()
      .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
      .setTranscoder(new WhalinTranscoder())
      .setOpTimeout(1000)
      .build();

    spyClient = new MemcachedClient(cf, Collections.singletonList(new InetSocketAddress("localhost", MEMCACHED_PORT)));
  }

  @AfterClass
  public static void teardownAfterClass() {
    spyClient.shutdown();
  }

  @Override
  protected TypedCache<String, Serializable> createCacheClient() throws Exception {
    final MetricFactory metricFactory = mock(MetricFactory.class, withSettings().defaultAnswer(RETURNS_MOCKS));
    final MemcacheClient<String, Serializable> clientDelegate = new MemcacheClient<>(spyClient, (CacheKeyTranslator<String>) key -> key, 1, TimeUnit.MINUTES);
    return new MonitoringCacheDelegate<>(clientDelegate, MemcacheClientTest.class.getSimpleName(), metricFactory);
  }

}
