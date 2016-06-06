package com.outbrain.ob1k.consul;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Eran Harel
 */
public class ConsulAPITest {
  private static final String SERVICE = "MockService";
  private static final String TAG1 = "tag1";
  private static final String TAG2 = "tag2";

  @Before
  public void setup() throws ExecutionException, InterruptedException {
    final ServiceRegistration.Check check = new ServiceRegistration.Check("\"http://localhost:8500/v1/", 60);
    final ServiceRegistration serviceRegistration = new ServiceRegistration(SERVICE, 8080,  Sets.newHashSet(TAG1, TAG2), check, 0);
    ConsulAPI.getServiceRegistry().register(serviceRegistration).get();
  }

  @After
  public void teardown() throws ExecutionException, InterruptedException {
    ConsulAPI.getServiceRegistry().deregister(SERVICE).get();
  }

  @Test
  public void testFilterDcLocalInstances() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().filterDcLocalInstances(SERVICE, TAG1).get();
    Assert.assertNotNull(instances);
    Assert.assertEquals(1, instances.size());
  }
}
