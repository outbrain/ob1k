package com.outbrain.ob1k.cache.memcache.folsom;

import com.google.common.collect.Lists;
import com.google.common.net.HostAndPort;
import com.outbrain.ob1k.cache.AbstractMemcachedClientTest;
import com.outbrain.ob1k.cache.CacheLoader;
import com.outbrain.ob1k.cache.LoadingCacheDelegate;
import com.outbrain.ob1k.cache.TypedCache;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.ErrorHandler;
import com.outbrain.ob1k.concurrent.handlers.FutureResultHandler;
import com.outbrain.ob1k.concurrent.handlers.ResultHandler;
import com.spotify.dns.DnsSrvResolver;
import com.spotify.dns.LookupResult;
import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import com.thimbleware.jmemcached.Cache;
import com.thimbleware.jmemcached.CacheElement;
import com.thimbleware.jmemcached.Key;
import com.thimbleware.jmemcached.MemCacheDaemon;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

  private static final Logger log = LoggerFactory.getLogger(MemcachedClientTest.class);
  private static final String PAYLOAD = "012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    startMemcached(11213);
    startMemcached(11214);

    MemcacheClient<Serializable> _client = MemcacheClientBuilder.newSerializableObjectClient()
      .withSRVRecord("memcached-WHATEVER")
      .withSrvResolver(new HealthyMemcachedResolver())
      .withSRVRefreshPeriod(1000)
      .withRequestTimeoutMillis(100)
      .withSRVShutdownDelay(10000)
//      .withAddress(HostAndPort.fromParts("localhost", 11213))
      .connectAscii();
    ConnectFuture.connectFuture(_client).get();

    final TypedCache<String, Serializable> client = new MemcachedClient<>(_client, key1 -> key1, 1, TimeUnit.MINUTES);
    CacheLoader<String, Serializable> loader = new CacheLoader<String, Serializable>() {
      @Override
      public ComposableFuture<Serializable> load(String cacheName, String key) {
        return ComposableFutures.schedule(() -> key + "-" + PAYLOAD, 10, TimeUnit.MILLISECONDS);
      }

      @Override
      public ComposableFuture<Map<String, Serializable>> load(String cacheName, Iterable<? extends String> keys) {
        final Map<String, Serializable> ret = StreamSupport.stream(keys.spliterator(), false).collect(Collectors.toMap(Function.identity(), k -> k + "-" + PAYLOAD));
        return ComposableFutures.schedule(() -> ret, 10, TimeUnit.MILLISECONDS);
      }
    };
    final TypedCache<String, Serializable> cache = new LoadingCacheDelegate<String, Serializable>(client, loader, "cacheName");

    final String key = "kkk";

    for (int i = 0; i < 100000000; i++) {
      try {
//        client.setAsync(key, i + "-" + PAYLOAD).get();
        final Serializable val = cache.getBulkAsync(Collections.singleton(key)).continueWith((ResultHandler<Map<String,Serializable>, Serializable>)res -> {
          return res.getError();
        }).get();

        if(i%10000 == 0) {
          log.info("Response={}", val);
        }
      } catch (final Exception e) {
        log.error(i + " " + e.getMessage());
      }
      Thread.sleep(1);
    }

  }

  private static void startMemcached(int port) {
    Cache<CacheElement> cache = new Cache<CacheElement>() {

      public volatile CacheElement e;

      @Override
      public DeleteResponse delete(Key key, int time) {
        return null;
      }

      @Override
      public StoreResponse add(CacheElement e) {
        return null;
      }

      @Override
      public StoreResponse replace(CacheElement e) {
        return null;
      }

      @Override
      public StoreResponse append(CacheElement element) {
        return null;
      }

      @Override
      public StoreResponse prepend(CacheElement element) {
        return null;
      }

      @Override
      public StoreResponse set(CacheElement e) {
        this.e = e;
        return StoreResponse.STORED;
      }

      @Override
      public StoreResponse cas(Long cas_key, CacheElement e) {
        return null;
      }

      @Override
      public Integer get_add(Key key, int mod) {
        return null;
      }

      private AtomicInteger count = new AtomicInteger();
      @Override
      public CacheElement[] get(Key... keys) {
        if (count.incrementAndGet() % 5000 == 0) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
          }
        }
        return new CacheElement[] {e};
      }

      @Override
      public boolean flush_all() {
        return false;
      }

      @Override
      public boolean flush_all(int expire) {
        return false;
      }

      @Override
      public void close() throws IOException {

      }

      @Override
      public long getCurrentItems() {
        return 0;
      }

      @Override
      public long getLimitMaxBytes() {
        return 0;
      }

      @Override
      public long getCurrentBytes() {
        return 0;
      }

      @Override
      public int getGetCmds() {
        return 0;
      }

      @Override
      public int getSetCmds() {
        return 0;
      }

      @Override
      public int getGetHits() {
        return 0;
      }

      @Override
      public int getGetMisses() {
        return 0;
      }

      @Override
      public Map<String, Set<String>> stat(String arg) {
        return null;
      }

      @Override
      public void asyncEventPing() {

      }
    };

    MemCacheDaemon<CacheElement> cacheDaemon = new MemCacheDaemon<>();
    cacheDaemon.setCache(cache);
    cacheDaemon.setAddr(new InetSocketAddress(port));
    cacheDaemon.setBinary(false);
    cacheDaemon.setVerbose(false);
    cacheDaemon.start();
  }

  private static class HealthyMemcachedResolver implements DnsSrvResolver {

    public static final ArrayList<LookupResult> NODES = Lists.newArrayList(LookupResult.create("192.168.30.243", 11213, 1, 1, 1000), LookupResult.create("192.168.30.243", 11214, 1, 1, 1000));

    private final AtomicInteger lookups = new AtomicInteger();

    @Override
    public List<LookupResult> resolve(String fqdn) {
      return NODES;
    }
  }

}
