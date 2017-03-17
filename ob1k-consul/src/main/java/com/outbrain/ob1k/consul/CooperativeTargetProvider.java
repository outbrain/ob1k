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
  private static final int PENALTY_ROUNDS = 2;
  private final Multiset<String> pendingRequests = ConcurrentHashMultiset.create();
  private final Multiset<String> remainingPenaltyRounds = ConcurrentHashMultiset.create();
  private final AtomicInteger roundRobin = new AtomicInteger();

  public CooperativeTargetProvider(final HealthyTargetsList healthyTargetsList, final String urlSuffix, final Map<String, Integer> tag2weight) {
    super(healthyTargetsList, urlSuffix, tag2weight);
  }

  @Override
  public List<String> provideTargetsImpl(final int targetsNum, final List<String> currTargets) {
    // We assume that only the first target is likely to be used, the other targets
    // may serve for double dispatch. That's why we increment the round robin counter
    // by only one.
    final int index = roundRobin.getAndIncrement();

    // get this target one round closer to being removed from the blacklist.
    remainingPenaltyRounds.remove(currTargets.get(Math.abs(index % currTargets.size())));

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

    Map<String, Integer> remainingPenaltyRounds = Maps.newHashMapWithExpectedSize(targetsNum);
    providedTargetsSet.forEach(target -> remainingPenaltyRounds.put(target, this.remainingPenaltyRounds.count(target)));

    return Ordering.from(
            Comparator.comparing(remainingPenaltyRounds::get).
                    thenComparing(pendingRequests::get).
                    thenComparing(roundRobinPosition::get)).
            leastOf(providedTargetsSet, targetsNum);
  }

  @Override
  public void onTargetsChanged(List<HealthInfoInstance> healthTargets) {
    super.onTargetsChanged(healthTargets);

    // Clear remainingPenaltyRounds, since we may never encounter
    // any of these targets again.
    remainingPenaltyRounds.clear();
  }

  public void targetDispatched(String target) {
    pendingRequests.add(target);
  }

  public void targetDispatchEnded(String target, boolean success) {
    pendingRequests.remove(target);
    if (success) {
      remainingPenaltyRounds.elementSet().remove(target);
    } else {
      remainingPenaltyRounds.setCount(target, PENALTY_ROUNDS);
    }
  }
}
