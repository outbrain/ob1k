package com.outbrain.gruffalo.publish;

import java.util.LinkedList;
import java.util.List;

/**
 * Time: 10/10/13 2:55 PM
 *
 * @author Eran Harel
 */
class CompoundMetricsPublisher implements MetricsPublisher {

  private final List<MetricsPublisher> publishers = new LinkedList<MetricsPublisher>();

  public CompoundMetricsPublisher(List<MetricsPublisher> publishers) {
    this.publishers.addAll(publishers);
  }

  @Override
  public void publishMetrics(final String payload) {
    for (MetricsPublisher publisher : publishers) {
      publisher.publishMetrics(payload);
    }
  }
}
