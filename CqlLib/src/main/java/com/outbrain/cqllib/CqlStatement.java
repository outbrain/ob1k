package com.outbrain.cqllib;

import com.datastax.driver.core.Statement;

import java.util.List;

/**
 * Created by guyk on 7/9/14.
 * a combination of a statement + tags for metrics
 */
public class CqlStatement {
  private final List<TagMetrics> metrics;
  private final Statement statement;


  CqlStatement(final Statement statement, final List<TagMetrics> metrics) {
    if (metrics.isEmpty()) {
      throw new IllegalArgumentException("expecting non empty tags");
    }
    this.statement = statement;
    this.metrics = metrics;
  }

  public List<TagMetrics> getTagMetrics() {
    return metrics;
  }

  public Statement getStatement() {
    return statement;
  }
}
