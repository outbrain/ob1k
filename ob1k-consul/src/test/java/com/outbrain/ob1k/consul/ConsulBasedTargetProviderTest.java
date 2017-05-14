package com.outbrain.ob1k.consul;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * @author marenzon
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsulBasedTargetProviderTest {

  private static final String MODULE_NAME = "MyService";
  private static final int PORT = 8080;

  @Mock
  private HealthyTargetsList healthyTargetsList;
  private ConsulBasedTargetProvider targetProvider;

  @Before
  public void init() {
    when(healthyTargetsList.getModule()).thenReturn(MODULE_NAME);
    targetProvider = new ConsulBasedTargetProvider(healthyTargetsList, "", emptyMap());
  }

  @Test
  public void testOnTargetsChange() {
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(1, true);
    targetProvider.onTargetsChanged(healthInfoInstances);

    assertEquals("onTargetsChanged should have updated targets list",
      createUrlFromTargetName(healthInfoInstances.get(0).Service.Address),
      targetProvider.provideTarget());
  }

  @Test
  public void testLogicalName() {
    assertEquals("logicalName should be same as module name", MODULE_NAME, targetProvider.getTargetLogicalName());
  }

  @Test
  public void testSingleProvideTarget() {
    // Initializing with single node
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(1, true);
    targetProvider.onTargetsChanged(healthInfoInstances);

    assertEquals("provided target should be same", createUrlFromTargetName(healthInfoInstances.get(0).Service.Address),
      targetProvider.provideTarget());
    assertEquals("both provided targets calls should return same target", targetProvider.provideTarget(),
      targetProvider.provideTarget());
  }

  @Test
  public void testMultipleProvideTarget() {
    // Initializing with multiple node
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(2, true);
    targetProvider.onTargetsChanged(healthInfoInstances);

    final String firstProvidedTarget = targetProvider.provideTarget();
    final String secondProvidedTarget = targetProvider.provideTarget();
    final String thirdProvidedTarget = targetProvider.provideTarget();

    assertEquals("first two provided targets should be same",
      createExpectedTargets(healthInfoInstances.get(0).Service.Address, healthInfoInstances.get(1).Service.Address),
      natural().sortedCopy(asList(firstProvidedTarget, secondProvidedTarget)));
    assertEquals("first and third provided targets should be same", firstProvidedTarget, thirdProvidedTarget);
  }

  @Test
  public void testSingleProvideTargets() {
    // Initializing with single node
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(1, true);
    targetProvider.onTargetsChanged(healthInfoInstances);

    // We have only one node, but result should contain it twice
    final List<String> targets = targetProvider.provideTargets(2);
    assertEquals("provided targets size should be two", 2, targets.size());
    assertEquals("provided target should be same",
      createUrlFromTargetName(healthInfoInstances.get(0).Service.Address), targets.iterator().next());
  }

  @Test
  public void testMultipleProvideTargets() {
    // Initializing with multiple node
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(2, true);
    targetProvider.onTargetsChanged(healthInfoInstances);

    final List<String> targets = targetProvider.provideTargets(2);

    assertEquals("provided targets size should be two", 2, targets.size());
    assertEquals("provided targets should be same",
      createExpectedTargets(healthInfoInstances.get(0).Service.Address, healthInfoInstances.get(1).Service.Address),
      natural().sortedCopy(targets));

    final List<String> moreTargets = targetProvider.provideTargets(2);
    assertEquals("first provided target should be same as second of previous one", targets.get(1), moreTargets.get(0));

    assertNotEquals("provide targets by 1 should return different targets", targetProvider.provideTargets(1),
      targetProvider.provideTargets(1));
  }

  @Test
  public void testTargetByNodeAddress() {
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(1, false);
    targetProvider.onTargetsChanged(healthInfoInstances);

    // We set the serviceAddress to be empty, thus expecting to fallback to nodeAddress
    assertEquals("onTargetsChanged should have updated targets list",
      createUrlFromTargetName(healthInfoInstances.get(0).Node.Address),
      targetProvider.provideTarget());
  }

  @Test
  public void testNullPortAndContextBase() {
    final List<HealthInfoInstance> healthInfoInstances = createHealthInfoInstances(1, true);
    healthInfoInstances.get(0).Service.Tags = null;
    targetProvider.onTargetsChanged(healthInfoInstances);

    for (int i = 0; i < 10; i++) {
      assertFalse("Port and context base parsing created an invlid url", targetProvider.provideTarget().contains("null"));
    }
  }

  private List<HealthInfoInstance> createHealthInfoInstances(final int numOfNodes, final boolean hasServiceAddress) {
    return IntStream.range(1, numOfNodes + 1).
      boxed().
      map(nodeIndex -> createHealthInfoInstance(hasServiceAddress, nodeIndex)).
      collect(Collectors.toList());
  }

  private HealthInfoInstance createHealthInfoInstance(final boolean hasServiceAddress, final int nodeIndex) {
    final HealthInfoInstance.Node node = new HealthInfoInstance.Node();
    node.Node = "myservice.node" + nodeIndex;
    node.Address = nodeIndex + "." + nodeIndex + "." + nodeIndex + "." + nodeIndex;

    final HealthInfoInstance.Service service = new HealthInfoInstance.Service();
    final String serviceIndex = nodeIndex + "0";
    service.Tags = newHashSet("httpPort-" + PORT, "contextPath-/" + MODULE_NAME);
    service.Address = Optional.of(serviceIndex + "." + serviceIndex + "." + serviceIndex + "." + serviceIndex).
      filter(__ -> hasServiceAddress).
      orElseGet(() -> "");

    final HealthInfoInstance instance = new HealthInfoInstance();

    instance.Node = node;
    instance.Service = service;
    instance.Checks = emptyList();

    return instance;
  }

  private static String createUrlFromTargetName(final String targetName) {
    return "http://" + targetName + ":" + PORT + "/" + MODULE_NAME;
  }

  private static List<String> createExpectedTargets(final String... targets) {
    return stream(targets).
      map(ConsulBasedTargetProviderTest::createUrlFromTargetName).
      sorted().
      collect(toList());
  }
}