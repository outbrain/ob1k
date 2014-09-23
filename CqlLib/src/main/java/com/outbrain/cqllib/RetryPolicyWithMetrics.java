package com.outbrain.cqllib;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.WriteType;
import com.datastax.driver.core.policies.RetryPolicy;
import com.google.common.base.Preconditions;

import java.util.List;

/**
 * Created by guyk on 7/9/14.
 */
class RetryPolicyWithMetrics implements RetryPolicy {
  private final RetryPolicy delegate;
  private final List<TagMetrics> tagMetrics;

  RetryPolicyWithMetrics(final RetryPolicy delegate, final List<TagMetrics> tagMetrics) {
    this.delegate = Preconditions.checkNotNull(delegate);
    this.tagMetrics = Preconditions.checkNotNull(tagMetrics);
  }

  @Override
  public RetryDecision onReadTimeout(final Statement statement, final ConsistencyLevel cl, final int requiredResponses,
                                     final int receivedResponses, final boolean dataRetrieved, final int nbRetry) {

    final RetryDecision decision = delegate.onReadTimeout(statement, cl, requiredResponses, receivedResponses, dataRetrieved, nbRetry);
    for (final TagMetrics c : tagMetrics) {
      c.readTimeouts.inc();
    }
    return decision;
  }

  @Override
  public RetryDecision onWriteTimeout(final Statement statement, final ConsistencyLevel cl, final WriteType writeType,
                                      final int requiredAcks, final int receivedAcks, final int nbRetry) {

    final RetryDecision decision = delegate.onWriteTimeout(statement, cl, writeType, requiredAcks, receivedAcks, nbRetry);
    for (final TagMetrics c : tagMetrics) {
      c.writeTimeouts.inc();
    }
    return decision;
  }

  @Override
  public RetryDecision onUnavailable(final Statement statement, final ConsistencyLevel cl, final int requiredReplica,
                                     final int aliveReplica, final int nbRetry) {

    final RetryDecision decision = delegate.onUnavailable(statement, cl, requiredReplica, aliveReplica, nbRetry);
    for (final TagMetrics c : tagMetrics) {
      c.unavailableTimeouts.inc();
    }
    return decision;
  }
}
