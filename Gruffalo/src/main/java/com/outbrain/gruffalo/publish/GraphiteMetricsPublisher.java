package com.outbrain.gruffalo.publish;

import com.google.common.base.Preconditions;
import com.outbrain.gruffalo.netty.GraphiteClient;

/**
 * Time: 7/28/13 3:16 PM
 *
 * @author Eran Harel
 */
class GraphiteMetricsPublisher implements MetricsPublisher {

  private final GraphiteClient graphiteClient;

  public GraphiteMetricsPublisher(final GraphiteClient graphiteClient) {
    this.graphiteClient = Preconditions.checkNotNull(graphiteClient, "graphiteClient must not be null");
  }

  @Override
  public void publishMetrics(final String metrics) {
    graphiteClient.publishMetrics(metrics);
  }

}
