package com.outbrain.ob1k.server.pushback;

import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;

@RunWith(MockitoJUnitRunner.class)
public class ConcurrencyLimitPushBackStrategyTest {

  @Mock
  private MetricFactory metricFactory;

  private ConcurrencyLimitPushBackStrategy strategy;


  @Before
  public void setup() {
    strategy = new ConcurrencyLimitPushBackStrategy(3, "component", metricFactory);
  }

  @Test
  public void shouldAllowConcurrentInvocationsUpToLimit() {
    // given
    final List<Boolean> results = new ArrayList<>();
    // when
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());

    // then
    Assert.assertEquals(asList(true, true, true, false), results);
  }


  @Test
  public void shouldAllowRequestsAfterLimitReachedAndRequestsWereDone() throws ExecutionException {
    // given
    final List<Boolean> results = new ArrayList<>();
    // when
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());
    results.add(strategy.allowRequest());
    strategy.done(false);
    strategy.done(true);
    results.add(strategy.allowRequest());

    // then
    Assert.assertEquals(asList(true, true, true, false, true), results);
  }
}