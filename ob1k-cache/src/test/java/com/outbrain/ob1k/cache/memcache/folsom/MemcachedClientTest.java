package com.outbrain.ob1k.cache.memcache.folsom;

import com.outbrain.ob1k.cache.AbstractMemcachedClientTest;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.cache.memcache.MemcacheClient;

import java.util.concurrent.TimeUnit;

/**
 * @author Eran Harel
 */
public class MemcachedClientTest extends AbstractMemcachedClientTest {

  @Override
  protected TypedCache<String, String> createCacheClient() throws Exception {
    return new MemcacheClient<>(getSpyClient(), key -> key, 1, TimeUnit.MINUTES);
  }
}
