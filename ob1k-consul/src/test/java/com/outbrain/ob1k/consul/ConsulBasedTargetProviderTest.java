package com.outbrain.ob1k.consul;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
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
    final String targetName = "myservice.node";
    targetProvider.onTargetsChanged(createServiceInstances(targetName));

    assertEquals("onTargetsChanged should have updated targets list", createUrlFromTargetName(targetName),
      targetProvider.provideTarget());
  }

  @Test
  public void testLogicalName() {
    assertEquals("logicalName should be same as module name", MODULE_NAME, targetProvider.getTargetLogicalName());
  }

  @Test
  public void testSingleProvideTarget() {
    // Initializing with single node
    final String targetName = "myservice.node";
    targetProvider.onTargetsChanged(createServiceInstances(targetName));

    assertEquals("provided target should be same", createUrlFromTargetName(targetName),
      targetProvider.provideTarget());
    assertEquals("both provided targets calls should return same target", targetProvider.provideTarget(),
      targetProvider.provideTarget());
  }

  @Test
  public void testMultipleProvideTarget() {
    // Initializing with multiple node
    final String firstTargetName = "myservice1.node";
    final String secondTargetName = "myservice2.node";
    targetProvider.onTargetsChanged(createServiceInstances(firstTargetName, secondTargetName));

    final String firstProvidedTarget = targetProvider.provideTarget();
    final String secondProvidedTarget = targetProvider.provideTarget();
    final String thirdProvidedTarget = targetProvider.provideTarget();

    assertEquals("first two provided targets should be same",
      createExpectedTargets(firstTargetName, secondTargetName),
      natural().sortedCopy(asList(firstProvidedTarget, secondProvidedTarget)));
    assertEquals("first and third provided targets should be same", firstProvidedTarget, thirdProvidedTarget);
  }

  @Test
  public void testSingleProvideTargets() {
    // Initializing with single node
    final String targetName = "myservice.node";
    targetProvider.onTargetsChanged(createServiceInstances(targetName));

    // We have only one node, but result should contain it twice
    final List<String> targets = targetProvider.provideTargets(2);
    assertEquals("provided targets size should be two", 2, targets.size());
    assertEquals("provided target should be same", createUrlFromTargetName(targetName), targets.iterator().next());
  }

  @Test
  public void testMultipleProvideTargets() {
    // Initializing with multiple node
    final String firstTargetName = "myservice1.node";
    final String secondTargetName = "myservice2.node";
    targetProvider.onTargetsChanged(createServiceInstances(firstTargetName, secondTargetName));

    final List<String> targets = targetProvider.provideTargets(2);

    assertEquals("provided targets size should be two", 2, targets.size());
    assertEquals("provided targets should be same", createExpectedTargets(firstTargetName, secondTargetName),
      natural().sortedCopy(targets));

    final List<String> moreTargets = targetProvider.provideTargets(2);
    assertEquals("first provided target should be same as second of previous one", targets.get(1), moreTargets.get(0));

    assertNotEquals("provide targets by 1 should return different targets", targetProvider.provideTargets(1),
      targetProvider.provideTargets(1));
  }

  private static List<HealthInfoInstance> createServiceInstances(final String... targetNames) {
    return stream(targetNames).map(targetName -> {
      final HealthInfoInstance.Node node = new HealthInfoInstance.Node();
      node.Node = targetName;

      final HealthInfoInstance.Service service = new HealthInfoInstance.Service();
      service.Tags = newHashSet("httpPort-" + PORT, "contextPath-/" + MODULE_NAME);

      final HealthInfoInstance instance = new HealthInfoInstance();

      instance.Node = node;
      instance.Service = service;
      instance.Checks = emptyList();

      return instance;
    }).collect(toList());
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