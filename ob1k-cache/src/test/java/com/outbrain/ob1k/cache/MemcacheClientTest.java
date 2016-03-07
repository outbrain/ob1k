package com.outbrain.ob1k.cache;

import com.outbrain.ob1k.cache.memcache.CacheKeyTranslator;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;
import com.outbrain.ob1k.cache.metrics.MonitoringCacheDelegate;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

/**
 * @author aronen
 */
public class MemcacheClientTest extends AbstractMemcachedClientTest {

  @Override
  protected TypedCache<String, String> createCacheClient() throws Exception {
    final MetricFactory metricFactory = mock(MetricFactory.class, withSettings().defaultAnswer(RETURNS_MOCKS));
    final MemcacheClient<String, String> clientDelegate = new MemcacheClient<>(getSpyClient(), (CacheKeyTranslator<String>) key -> key, 1, TimeUnit.MINUTES);
    return new MonitoringCacheDelegate<>(clientDelegate, MemcacheClientTest.class.getSimpleName(), metricFactory);
  }

}
