package com.outbrain.ob1k.consul;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableSet;

/**
 * Created by ahadadi on 18/03/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class CooperativeTargetProviderTest {

  private static final List<String> TARGETS = asList("t1", "t2", "t3");
  private static final int PENALTY = 2;

  @Mock
  private HealthyTargetsList healthyTargetsList;

  private CooperativeTargetProvider provider;

  @Before
  public void setup() {
    provider = new CooperativeTargetProvider(healthyTargetsList, "urlSuffix", Collections.singletonMap("tag", 1), PENALTY);
  }

  @Test
  public void testReturnByNumberOfPendingRequests() {
    provider.targetDispatched("t1");
    assertEquals(ImmutableSet.of("t2", "t3"), ImmutableSet.copyOf(provider.provideTargetsImpl(2, TARGETS)));
  }

  @Test
  public void testPenaltyDecay() {
    provider.targetDispatchEnded("t1", false);
    assertEquals(ImmutableSet.of("t2"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    provider.targetDispatched("t2");
    assertEquals(ImmutableSet.of("t3"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    provider.targetDispatched("t3");
    assertEquals(ImmutableSet.of("t3"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    provider.targetDispatched("t3");
    assertEquals(ImmutableSet.of("t1"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
  }

  @Test
  public void testRoundRobin() {
    assertEquals(ImmutableSet.of("t1"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(ImmutableSet.of("t2"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(ImmutableSet.of("t3"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));

    assertEquals(ImmutableSet.of("t1"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(ImmutableSet.of("t2"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(ImmutableSet.of("t3"), ImmutableSet.copyOf(provider.provideTargetsImpl(1, TARGETS)));
  }


}