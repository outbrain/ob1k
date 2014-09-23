package com.outbrain.cqllib;

import com.datastax.driver.core.Statement;
import com.google.common.collect.Lists;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.List;

/**
 * Created by guyk on 7/13/14.
 */
public class CqlStatementFactory {
  private final List<TagMetrics> tagMetrics = Lists.newArrayList();

  CqlStatementFactory(final MetricFactory metricFactory, final String... tags) {
    if (tags.length == 0) {
      throw new IllegalArgumentException("expecting non empty tags");
    }
    for (final String tag : tags) {
      tagMetrics.add(new TagMetrics(metricFactory, tag));
    }
  }

  public CqlStatement createStatement(final Statement statement) {
    return new CqlStatement(statement, tagMetrics);
  }

}
