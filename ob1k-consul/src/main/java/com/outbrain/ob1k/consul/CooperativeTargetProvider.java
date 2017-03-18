package com.outbrain.ob1k.consul;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import com.outbrain.ob1k.client.targets.TargetProvider;

/**
 * A {@link TargetProvider} that chooses targets likely to respond fast and successfully.
 *
 * @author Amir Hadadi
 */
public class CooperativeTargetProvider extends ConsulBasedTargetProvider {

  private static final Logger log = LoggerFactory.getLogger(CooperativeTargetProvider.class);
  private static final int EXTRA_TARGETS = 10;
  private final int penalty;
  private final Multiset<String> pendingRequests = ConcurrentHashMultiset.create();
  private final Multiset<String> remainingPenalty = ConcurrentHashMultiset.create();
  private final AtomicInteger roundRobin = new AtomicInteger();

  public CooperativeTargetProvider(final HealthyTargetsList healthyTargetsList, final String urlSuffix, final Map<String, Integer> tag2weight, final int penalty) {
    super(healthyTargetsList, urlSuffix, tag2weight);
    this.penalty = penalty;
  }

  @Override
  public List<String> provideTargetsImpl(final int targetsNum, final List<String> currTargets) {
    // We assume that only the first target is likely to be used, the other targets
    // may serve for double dispatch. That's why we increment the round robin counter
    // by only one.
    final int index = roundRobin.getAndIncrement();

    // get this target closer to being removed from the blacklist.
    remainingPenalty.remove(currTargets.get(Math.abs(index % currTargets.size())));

    // Maintain the order of the elements so that we can tie break them
    // by the round robin order.
    final Set<String> providedTargetsSet = new LinkedHashSet<>();

    final int targetsPool = Math.min(targetsNum + EXTRA_TARGETS, currTargets.size());
    for (int i = 0; i < targetsPool; i++) {
      providedTargetsSet.add(currTargets.get(Math.abs((index + i) % currTargets.size())));
    }

    final List<String> providedTargets = bestTargets(targetsNum, providedTargetsSet);

    for (int i=0; providedTargets.size() < targetsNum; i++) {
      providedTargets.add(providedTargets.get(i % providedTargets.size()));
    }

    return providedTargets;
  }

  private List<String> bestTargets(int targetsNum, Set<String> providedTargetsSet) {
    // create snapshots so that the comparator will not switch the order of targets during sorting.
    Map<String, Integer> pendingRequests = Maps.newHashMapWithExpectedSize(targetsNum);
    providedTargetsSet.forEach(target -> pendingRequests.put(target, this.pendingRequests.count(target)));

    Map<String, Integer> roundRobinPosition = Maps.newHashMapWithExpectedSize(targetsNum);
    providedTargetsSet.forEach(target -> roundRobinPosition.put(target, roundRobinPosition.size()));

    Map<String, Integer> remainingPenalty = Maps.newHashMapWithExpectedSize(targetsNum);
    providedTargetsSet.forEach(target -> remainingPenalty.put(target, this.remainingPenalty.count(target)));

    return Ordering.from(Comparator.<String, Integer>comparing(target -> remainingPenalty.get(target) + pendingRequests.get(target)).
                    thenComparing(roundRobinPosition::get)).
            leastOf(providedTargetsSet, targetsNum);
  }

  @Override
  public void onTargetsChanged(List<HealthInfoInstance> healthTargets) {
    super.onTargetsChanged(healthTargets);

    // Clear remainingPenalty, since we may never encounter
    // any of these targets again.
    remainingPenalty.clear();
  }

  public void targetDispatched(String target) {
    pendingRequests.add(target);
  }

  public void targetDispatchEnded(String target, boolean success) {
    pendingRequests.remove(target);
    if (success) {
      remainingPenalty.elementSet().remove(target);
    } else {
      remainingPenalty.setCount(target, penalize(target));
    }
  }

  private int penalize(String target) {
    final int maxPendingRequests = pendingRequests.entrySet().stream().
            mapToInt(Multiset.Entry::getCount).
            max().orElse(0);

    return penalty + Math.max(maxPendingRequests - pendingRequests.count(target), 0);
  }
}
