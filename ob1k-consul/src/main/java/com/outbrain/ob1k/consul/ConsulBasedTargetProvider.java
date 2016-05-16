package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.client.targets.TargetProvider;
import io.netty.util.internal.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link TargetProvider} that provides targets registered in consul.
 *
 * @author Eran Harel
 */
public class ConsulBasedTargetProvider implements TargetProvider, HealthyTargetsList.TargetsChangedListener {

  private static final Logger log = LoggerFactory.getLogger(ConsulBasedTargetProvider.class);

  private final ThreadLocal<Integer> currIndex = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return ThreadLocalRandom.current().nextInt();
    }
  };

  private final String urlSuffix;
  private final Map<String, Integer> tag2weight;
  private final HealthyTargetsList healthyTargetsList;
  private volatile List<String> targets;

  public ConsulBasedTargetProvider(final HealthyTargetsList healthyTargetsList, final String urlSuffix, final Map<String, Integer> tag2weight) {
    this.urlSuffix = urlSuffix == null ? "" : urlSuffix;
    this.tag2weight = tag2weight == null ? Collections.<String, Integer>emptyMap() : new HashMap<>(tag2weight);
    this.healthyTargetsList = Preconditions.checkNotNull(healthyTargetsList, "healthyTargetsList must not be null");
    healthyTargetsList.addListener(this);
  }

  @Override
  public String getTargetLogicalName() {
    return healthyTargetsList.getModule();
  }

  @Override
  public String provideTarget() {
    if (targets.isEmpty()) {
      throw new IllegalStateException("No targets are currently registered for module " + healthyTargetsList.getModule());
    }

    final int index = currIndex.get();
    currIndex.set(index + 1);
    final List<String> currTargets = targets;
    final String target = currTargets.get(Math.abs(index % currTargets.size()));
    log.debug("target provided: {}", target);
    return target;
  }

  private int instanceWeight(final HealthInfoInstance.Service instance) {
    for (final Map.Entry<String, Integer> tagWeight : tag2weight.entrySet()) {
      if (instance.Tags.contains(tagWeight.getKey())) {
        return tagWeight.getValue();
      }
    }

    return 1;
  }

  @Override
  public void onTargetsChanged(final List<HealthInfoInstance> healthTargets) {
    final List<String> targets = new ArrayList<>(healthTargets.size());
    for (final HealthInfoInstance healthInfo : healthTargets) {
      final String targetUrl = "http://" + healthInfo.Node.Node + ":" + healthInfo.Service.port("http") + healthInfo.Service.context() + urlSuffix;
      final int weight = instanceWeight(healthInfo.Service);
      for (int i = 0; i < weight; i++) {
        targets.add(targetUrl);
      }
    }

    Collections.shuffle(targets);
    this.targets = targets;
    log.debug("New weighed targets: {}", targets);
  }
}
