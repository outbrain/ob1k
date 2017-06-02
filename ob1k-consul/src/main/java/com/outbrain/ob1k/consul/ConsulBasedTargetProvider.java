package com.outbrain.ob1k.consul;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.client.targets.TargetProvider;

import io.netty.util.internal.ThreadLocalRandom;

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
    return provideTargets(1).get(0); // knowing that provideTargets may not return empty collection
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
  public List<String> provideTargets(final int targetsNum) {
    final List<String> currTargets = targets;

    checkState(!currTargets.isEmpty(), "No targets are currently registered for module " + healthyTargetsList.getModule());
    checkArgument(targetsNum > 0, "targets number must be more than zero");

    return provideTargetsImpl(targetsNum, currTargets);
  }

  protected List<String> provideTargetsImpl(int targetsNum, List<String> currTargets) {
    final int targetsSize = currTargets.size();
    final int index = currIndex.get();

    currIndex.set(index + 1);

    final List<String> providedTargets = new LinkedList<>();
    for (int i = 0; i < targetsNum; i++) {
      providedTargets.add(currTargets.get(Math.abs((index + i) % targetsSize)));
    }

    return providedTargets;
  }

  @Override
  public void onTargetsChanged(final List<HealthInfoInstance> healthTargets) {
    final List<String> targets = new ArrayList<>(healthTargets.size());
    for (final HealthInfoInstance healthInfo : healthTargets) {
      final String targetUrl = createTargetUrl(healthInfo);
      final int weight = instanceWeight(healthInfo.Service);
      for (int i = 0; i < weight; i++) {
        targets.add(targetUrl);
      }
    }

    Collections.shuffle(targets);
    this.targets = targets;
    log.debug("New weighed targets: {}", targets);
  }

  private String createTargetUrl(final HealthInfoInstance healthInfo) {
    final String nodeAddress = healthInfo.Node.Address;
    final String serviceAddress = healthInfo.Service.Address;
    Integer port = healthInfo.Service.port("http");
    String contextBase = healthInfo.Service.context();
    // Take service address (IP) unless it is null/empty - then take the node address (IP)
    final String targetAddress = Optional.ofNullable(serviceAddress).filter(StringUtils::isNotBlank).orElse(nodeAddress);

    // screw this, but at least it will work for nor non standard registrations
    if (null == port) {
      log.error("http port tag is null or missing for {}", targetAddress);
      port = healthInfo.Service.Port;
    }
    if (null == contextBase) {
      log.info("contextPath tag is null or missing for {}", targetAddress);
      contextBase = "";
    }

    return "http://" + targetAddress + ":" + port + contextBase + urlSuffix;
  }

}
