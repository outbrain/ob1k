package com.outbrain.ob1k.consul;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author Eran Harel
 */
@Ignore
public class ConsulAPITest {
  private static final String SERVICE1_NAME = "MockService1";
  private static final String SERVICE2_NAME = "MockService2";
  private static final String TAG1 = "tag1";
  private static final String TAG2 = "tag2";
  private static final ServiceRegistration.Check passingCheck = new PassingCheck();
  private static final ServiceRegistration SERVICE1_REGISTRATION = new ServiceRegistration(SERVICE1_NAME, 8080,  Sets.newHashSet(TAG1, TAG2), passingCheck, 0);
  private static final ServiceRegistration SERVICE2_REGISTRATION = new ServiceRegistration(SERVICE2_NAME, 8080,  Sets.newHashSet(TAG1), passingCheck, 0);

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

  @Test
  public void testDatacenters() throws ExecutionException, InterruptedException {
    final Set<String> dcs = ConsulAPI.getCatalog().datacenters().get();
    Assert.assertNotNull(dcs);
    Assert.assertEquals(1, dcs.size());
    Assert.assertEquals("dc1", dcs.iterator().next());
  }

  @Test
  public void testFetchInstancesHealth_fetchHealthyInstance() throws ExecutionException, InterruptedException {
    final List<HealthInfoInstance> service1Health = ConsulAPI.getHealth().fetchInstancesHealth(SERVICE1_NAME, "").get();
    Assert.assertNotNull(service1Health);
    Assert.assertEquals(1, service1Health.size());
  }

  @Test
  public void testFilterDcLocalHealthyInstances_shouldFindWhenTagExists() throws ExecutionException, InterruptedException {
    final List<HealthInfoInstance> service1Health = ConsulAPI.getHealth().filterDcLocalHealthyInstances(SERVICE1_NAME, TAG1).get();
    Assert.assertNotNull(service1Health);
    Assert.assertEquals(1, service1Health.size());
  }

  @Test
  public void testFilterDcLocalHealthyInstances_shouldNotFindWhenTagMisses() throws ExecutionException, InterruptedException {
    final List<HealthInfoInstance> service1Health = ConsulAPI.getHealth().filterDcLocalHealthyInstances(SERVICE2_NAME, TAG2).get();
    Assert.assertNotNull(service1Health);
    Assert.assertTrue(service1Health.isEmpty());
  }

  @Test
  public void testEnableMaintenance() throws ExecutionException, InterruptedException {
    ConsulAPI.getServiceRegistry().enableMaintenance(SERVICE1_REGISTRATION.getID(), "fail health check").get();
    final List<HealthInfoInstance> service1Health = ConsulAPI.getHealth().filterDcLocalHealthyInstances(SERVICE1_NAME, TAG1).get();
    Assert.assertNotNull(service1Health);
    Assert.assertTrue(service1Health.isEmpty());
  }

  @Test
  public void testDisableMaintenance() throws ExecutionException, InterruptedException {
    ConsulAPI.getServiceRegistry().enableMaintenance(SERVICE1_REGISTRATION.getID(), "fail health check").get();
    ConsulAPI.getServiceRegistry().disableMaintenance(SERVICE1_REGISTRATION.getID()).get();
    final List<HealthInfoInstance> service1Health = ConsulAPI.getHealth().filterDcLocalHealthyInstances(SERVICE1_NAME, TAG1).get();
    Assert.assertNotNull(service1Health);
    Assert.assertEquals(1, service1Health.size());
  }

  private static class PassingCheck extends ServiceRegistration.Check {
    public String getHttp() {
      return null; // disable (hack) the standard HTTP check....
    }
    private String getScript() { return "echo PASSING";}
  }
}