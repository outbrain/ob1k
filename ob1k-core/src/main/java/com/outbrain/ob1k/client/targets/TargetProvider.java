package com.outbrain.ob1k.client.targets;

import java.util.List;

/**
 * An API for retrieving targets for the client invocations.
 * An example target may be a static list of hosts + round robin, or a discovery based provider.
 *
 * @author eran 6/21/15.
 */
public interface TargetProvider {
  /**
   * Returns logical name of the target provider (e.g. client name).
   *
   * @return target logical name
   */
  String getTargetLogicalName();

  /**
   * Returns remote target for invocation.
   *
   * @return target host
   */
  String provideTarget();

  /**
   * Returns collection of remote targets for invocation for size specified.
   * May return collection with duplicate targets, and the collection size will always
   * be as requested
   *
   * @param targetsNum number of targets to return
   * @return targets hosts
   */
  List<String> provideTargets(int targetsNum);

  /**
   * Should be called before dispatching.
   * Allows for load balancing decisions - e.g. counting the number of pending requests to each target.
   *
   * @param target host
   */
  default void targetDispatched(String target) {
  }

  /**
   * Should be called when dispatch completes.
   * Allows for load balancing decisions - e.g. counting the number of pending requests to each target,
   * and monitoring targets latency.
   *
   * @param target          host
   * @param success         whether the dispatch failed or succeeded
   * @param startTimeNanos: dispatch start time in nanoseconds
   */
  default void targetDispatchEnded(String target, boolean success, final long startTimeNanos) {
  }
}
