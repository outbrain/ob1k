package com.outbrain.ob1k.consul;

import io.netty.util.internal.ThreadLocalRandom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A {@link LoadBalancer} using round robin to dispense targets.
 * The round robin is done per thread, trading off load balancing
 * accuracy for contention elimination.
 *
 * @author Eran Harel
 */
public class ThreadLocalRoundRobinLoadBalancer implements LoadBalancer {

  private volatile List<String> targets;

  private final ThreadLocal<Integer> currIndex = ThreadLocal.withInitial(() -> ThreadLocalRandom.current().nextInt());

  @Override
  public List<String> provideTargets(final int targetsNum) {
    final List<String> currTargets = targets;

    if (currTargets == null || currTargets.isEmpty()) {
      return Collections.emptyList();
    }

    final int targetsSize = currTargets.size();
    final int index = currIndex.get();

    currIndex.set(index + 1);

    final List<String> providedTargets = new ArrayList<>(targetsNum);
    for (int i = 0; i < targetsNum; i++) {
      providedTargets.add(currTargets.get(Math.abs((index + i) % targetsSize)));
    }

    return providedTargets;
  }

  @Override
  public void onTargetsChanged(final List<String> newTargets) {
    this.targets = newTargets;
  }
}
