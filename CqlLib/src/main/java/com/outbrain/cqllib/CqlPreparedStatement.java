package com.outbrain.cqllib;

import com.datastax.driver.core.PreparedStatement;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by guyk on 7/10/14.
 */
public class CqlPreparedStatement {

  private final List<TagMetrics> metrics;
  private final PreparedStatement statement;

  public CqlPreparedStatement(final PreparedStatement statement, final MetricFactory metricFactory, final String... tags) {
    if (tags.length == 0) {
      throw new IllegalArgumentException("expecting non empty tags");
    }
    this.statement = Preconditions.checkNotNull(statement);
    this.metrics = Lists.newArrayList(Iterables.transform(Arrays.asList(tags), new Function<String, TagMetrics>() {
      @Override
      public TagMetrics apply(String tag) {
        return new TagMetrics(metricFactory, tag);
      }
    }));
  }

  public List<TagMetrics> getTagMetrics() {
    return metrics;
  }

  public PreparedStatement getStatement() {
    return statement;
  }
}
