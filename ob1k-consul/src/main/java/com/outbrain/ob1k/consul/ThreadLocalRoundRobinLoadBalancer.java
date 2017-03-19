package com.outbrain.ob1k.consul;

import java.util.ArrayList;
import java.util.List;

import io.netty.util.internal.ThreadLocalRandom;

/**
 * A {@link LoadBalancer} using round robin to dispense targets.
 * The round robin is done per thread, trading off load balancing
 * accuracy for contention elimination.
 *
 * @author Eran Harel
 */
public class ThreadLocalRoundRobinLoadBalancer implements LoadBalancer {

  private final ThreadLocal<Integer> currIndex = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return ThreadLocalRandom.current().nextInt();
    }
  };

  @Override
  public List<String> provideTargets(final List<String> targets, final int targetsNum) {
    final int targetsSize = targets.size();
    final int index = currIndex.get();

    currIndex.set(index + 1);

    final List<String> providedTargets = new ArrayList<>(targetsNum);
    for (int i = 0; i < targetsNum; i++) {
      providedTargets.add(targets.get(Math.abs((index + i) % targetsSize)));
    }

    return providedTargets;
  }
}
