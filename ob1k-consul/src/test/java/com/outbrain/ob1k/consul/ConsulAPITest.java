package com.outbrain.ob1k.consul;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author Eran Harel
 */
public class ConsulAPITest {
  private static final String SERVICE1_NAME = "MockService1";
  private static final String SERVICE2_NAME = "MockService2";
  private static final String TAG1 = "tag1";
  private static final String TAG2 = "tag2";
  private static final ServiceRegistration.Check check = new ServiceRegistration.Check("\"http://localhost:8500/v1/", 60);
  private static final ServiceRegistration SERVICE1_REGISTRATION = new ServiceRegistration(SERVICE1_NAME, 8080,  Sets.newHashSet(TAG1, TAG2), check, 0);
  private static final ServiceRegistration SERVICE2_REGISTRATION = new ServiceRegistration(SERVICE2_NAME, 8080,  Sets.newHashSet(TAG1), check, 0);

  @Before
  public void setup() throws ExecutionException, InterruptedException {
    ConsulAPI.getServiceRegistry().register(SERVICE1_REGISTRATION).get();
    ConsulAPI.getServiceRegistry().register(SERVICE2_REGISTRATION).get();
  }

  @After
  public void teardown() throws ExecutionException, InterruptedException {
    ConsulAPI.getServiceRegistry().deregister(SERVICE1_REGISTRATION.getID()).get();
    ConsulAPI.getServiceRegistry().deregister(SERVICE2_REGISTRATION.getID()).get();
  }

  @Ignore // TODO fix
  @Test
  public void testFindDcLocalInstances() throws ExecutionException, InterruptedException, IOException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().findDcLocalInstances(SERVICE2_NAME).get();
    Assert.assertNotNull(instances);
    Assert.assertEquals(1, instances.size());
  }

  @Test
  public void testFilterDcLocalInstances_shouldFindWhenTagExists() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().filterDcLocalInstances(SERVICE1_NAME, TAG1).get();
    Assert.assertNotNull(instances);
    Assert.assertEquals(1, instances.size());
  }

  @Test
  public void testFilterDcLocalInstances_shouldMissWhenTagIsMissing() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().filterDcLocalInstances(SERVICE2_NAME, TAG2).get();
    Assert.assertNotNull(instances);
    Assert.assertTrue(instances.isEmpty());
  }

  @Test
  public void testFindInstances_shouldFindInDefaultDCWhenDCIsNotSpecified() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().findInstances(SERVICE1_NAME, "").get();
    Assert.assertNotNull(instances);
    Assert.assertEquals(1, instances.size());
  }

  @Test
  public void testFindInstances_shouldFindWhenDCIsSpecified() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().findInstances(SERVICE1_NAME, "dc1").get();
    Assert.assertNotNull(instances);
    Assert.assertEquals(1, instances.size());
  }

  @Test
  public void testFindInstances_shouldFindNothingForBogusService() throws ExecutionException, InterruptedException {
    final List<ServiceInstance> instances = ConsulAPI.getCatalog().findInstances("ImAmNotAService", "dc1").get();
    Assert.assertNotNull(instances);
    Assert.assertTrue(instances.isEmpty());
  }
}
