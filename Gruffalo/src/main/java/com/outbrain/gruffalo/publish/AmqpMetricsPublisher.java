package com.outbrain.gruffalo.publish;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.util.Assert;

class AmqpMetricsPublisher implements MetricsPublisher {

  private final AmqpTemplate amqpTemplate;

  public AmqpMetricsPublisher(final AmqpTemplate amqpTemplate) {
    Assert.notNull(amqpTemplate, "amqpTemplate may not be null");
    this.amqpTemplate = amqpTemplate;
  }

  @Override
  public void publishMetrics(final String payload) {
    amqpTemplate.convertAndSend(payload);
  }
}
