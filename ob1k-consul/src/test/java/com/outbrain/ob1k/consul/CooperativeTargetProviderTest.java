package com.outbrain.ob1k.consul;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by ahadadi on 18/03/2017.
 */
@RunWith(MockitoJUnitRunner.class)
public class CooperativeTargetProviderTest {

  private static final List<String> TARGETS = asList("t1", "t2");
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
    assertEquals(singletonList("t2"), provider.provideTargetsImpl(1, TARGETS));
  }

  @Test
  public void testReturnTheSameTargetMultipleTimes() {
    assertEquals(asList("t1", "t2", "t1"), provider.provideTargetsImpl(3, TARGETS));
  }

  @Test
  public void testClearPenaltiesOnTargetsChange() {
    provider.targetDispatchEnded("t1", false);
    provider.onTargetsChanged(Collections.emptyList());
    assertEquals(singletonList("t1"), provider.provideTargetsImpl(1, TARGETS));
  }

  @Test
  public void testPenaltyDecay() {
    provider.targetDispatchEnded("t1", false);
    IntStream.range(0, PENALTY).forEach(i -> assertEquals(singletonList("t2"), provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(singletonList("t1"), provider.provideTargetsImpl(1, TARGETS));
  }

  @Test
  public void testDecreasePenaltyWhenTheMaximumPendingRequestsDecreases() {
    final int requests = 10;
    IntStream.range(0, requests).forEach(i -> provider.targetDispatched("t2"));

    // t1 should now have a high penalty to make it non competitive with t2.
    provider.targetDispatchEnded("t1", false);

    // t1's penalty should decrease as t2 responded for all requests.
    IntStream.range(0, requests).forEach(i -> provider.targetDispatchEnded("t2", true));

    IntStream.range(0, PENALTY).forEach(i -> assertEquals(singletonList("t2"), provider.provideTargetsImpl(1, TARGETS)));
    assertEquals(singletonList("t1"), provider.provideTargetsImpl(1, TARGETS));
  }

  @Test
  public void testRoundRobin() {
    assertEquals(singletonList("t1"), provider.provideTargetsImpl(1, TARGETS));
    assertEquals(singletonList("t2"), provider.provideTargetsImpl(1, TARGETS));

    assertEquals(singletonList("t1"), provider.provideTargetsImpl(1, TARGETS));
    assertEquals(singletonList("t2"), provider.provideTargetsImpl(1, TARGETS));
  }
}