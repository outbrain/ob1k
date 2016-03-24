package com.outbrain.ob1k.cache.memcache.folsom;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.consul.ConsulHealth;
import com.outbrain.ob1k.consul.HealthInfoInstance;
import com.outbrain.ob1k.consul.HealthyTargetsList;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.swinfra.metrics.codahale3.CodahaleMetricsFactory;
import com.spotify.dns.LookupResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Test cases for {@link HealthyMemcachedResolver}.
 *
 * @author Eran Harel
 */
@RunWith(MockitoJUnitRunner.class)
public class HealthyMemcachedResolverTest {

  @Mock
  private ConsulHealth mockHealth;
  @Mock
  private TypedResponse<List<HealthInfoInstance>> mockResponse;

  private final HealthInfoInstance memcachedInstance = createMemcachedNode();
  private final List<HealthInfoInstance> healthyInstances = Lists.newArrayList(memcachedInstance);

  private HealthyMemcachedResolver resolver;
  public static final String SERVICE = "memcached";

  @Before
  public void setup() throws ExecutionException, InterruptedException {
    final String filterTag = "filterTag";
    Mockito.when(mockHealth.filterDcLocalHealthyInstances(SERVICE, filterTag)).thenReturn(ComposableFutures.fromValue(healthyInstances));
    Mockito.when(mockHealth.pollHealthyInstances(SERVICE, filterTag, 0)).thenReturn(ComposableFutures.schedule(() -> mockResponse, 1, TimeUnit.HOURS));
    final HealthyTargetsList healthyTargetsList = new HealthyTargetsList(mockHealth, SERVICE, filterTag, null, new CodahaleMetricsFactory(new MetricRegistry()));

    resolver = new HealthyMemcachedResolver(healthyTargetsList);
    healthyTargetsList.getInitializationFuture().get();
  }

  private HealthInfoInstance createMemcachedNode() {
    final HealthInfoInstance node = new HealthInfoInstance();
    node.Node = new HealthInfoInstance.Node();
    node.Node.Address = "memcached01";
    node.Service = new HealthInfoInstance.Service();
    node.Service.Port = 11911;

    return node;
  }

  @Test
  public void testResolve() {
    final List<LookupResult> lookupResults = resolver.resolve(SERVICE);
    Assert.assertNotNull("lookupResults", lookupResults);
    Assert.assertEquals("num results", 1, lookupResults.size());
    final LookupResult lookupResult = lookupResults.get(0);
    Assert.assertEquals("host", memcachedInstance.Node.Address, lookupResult.host());
    Assert.assertEquals("port", memcachedInstance.Service.Port, lookupResult.port());
  }
}
