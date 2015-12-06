package com.outbrain.swinfra.metrics;

import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.mockito.Answers;
import org.mockito.Mockito;

public class DummyMetricFactory {

  public static MetricFactory newDummyMetricFactory() {
    return Mockito.mock(MetricFactory.class, Answers.RETURNS_MOCKS.get());
  }

}
