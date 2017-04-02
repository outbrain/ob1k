package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;
import com.outbrain.ob1k.consul.filter.AllTargetsPredicate;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Histogram;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Maintains a list of healthy targets based on consul registration.
 * The updated list can be retrieved using a {@link TargetsChangedListener}
 * provided to the {@link #addListener(TargetsChangedListener)} method, or by calling {@link #getHealthyInstances()}.
 *
 * @author Eran Harel
 */
public class HealthyTargetsList {

  private static final Logger log = LoggerFactory.getLogger(HealthyTargetsList.class);
  /*
   * NOTE: an assumption is being made here:
   * The targets are always initialized with the last successful value.
   * Errors are pushed to the logs and counters, and not propagated to the clients.
   * This is to allow making this class lazy, and allow smoother startups where needed.
   */
  private volatile ComposableFuture<List<HealthInfoInstance>> healthyTargetsFuture = ComposableFutures.fromValue(Collections.<HealthInfoInstance>emptyList());

  private final List<TargetsChangedListener> listeners = new CopyOnWriteArrayList<>();
  private final String module;

  private final String envTag;
  private final Predicate<HealthInfoInstance> targetsPredicate;
  private final ConsulHealth health;

  private final Timer targetFetchTime;
  private final Counter targetFetchErrors;
  private final Counter targetUpdates;
  private final Counter targetUpdateSkip;
  private final Histogram newTargetsSize;
  private final Histogram filteredTargetsSize;

  private final Callable<?> retryPoll = () -> {
    pollForTargetsUpdates(0, Collections.emptyMap());
    return null;
  };

  private final ComposablePromise<?> initializationFuture = ComposableFutures.newPromise(false);

  public HealthyTargetsList(final ConsulHealth health, final String module, final String envTag, final Predicate<HealthInfoInstance> targetsPredicate, final MetricFactory metricFactory) {
    this.health = Preconditions.checkNotNull(health, "health must not be null");
    this.module = Preconditions.checkNotNull(module, "module must not be null");
    this.envTag = Preconditions.checkNotNull(envTag, "envTag must not be null");
    this.targetsPredicate = (targetsPredicate == null ? AllTargetsPredicate.INSTANCE : targetsPredicate);

    Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
    final String component = getClass().getSimpleName() + "." + module;

    targetFetchTime = metricFactory.createTimer(component, "targetFetchTime");
    targetFetchErrors = metricFactory.createCounter(component, "targetFetchErrors");
    targetUpdates = metricFactory.createCounter(component, "targetUpdates");
    targetUpdateSkip = metricFactory.createCounter(component, "targetUpdateSkip");
    newTargetsSize = metricFactory.createHistogram(component, "newTargetsSize", false);
    filteredTargetsSize = metricFactory.createHistogram(component, "filteredTargetsSize", false);

    metricFactory.registerGauge(component, "targetCount", this::getHealthyInstancesCount);

    initTargetsAsync();
  }

  /**
   * @return a future that can be waited for (or being notified on) to ensure initialization completed.
   */
  public ComposableFuture<?> getInitializationFuture() {
    return initializationFuture.future();
  }

  private Integer getHealthyInstancesCount() {
    try {
      return healthyTargetsFuture.get().size();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return 0;
    } catch (final ExecutionException e) {
      return 0;
    }
  }

  private void notifyListeners(final List<HealthInfoInstance> newTargets) {
    for (final TargetsChangedListener listener : listeners) {
      listener.onTargetsChanged(newTargets);
    }
  }

  private Map<InstanceKey, Long> setTargetsIfChanged(final List<HealthInfoInstance> newTargets, final Map<InstanceKey, Long> lastInstance2modifyIndex){
    final List<HealthInfoInstance> filteredTargets =
      Collections.unmodifiableList(newTargets.stream().filter(targetsPredicate::apply).collect(Collectors.toList()));
    final Map<InstanceKey, Long> currentInstance2modifyIndex = extractModifyIndices(filteredTargets);

    log.debug("received new targets for module {}, size of filtered: {}, size of received: {}", module,
      filteredTargets.size(), newTargets.size());

    newTargetsSize.update(newTargets.size());
    filteredTargetsSize.update(filteredTargets.size());

    if (lastInstance2modifyIndex.equals(currentInstance2modifyIndex)) {
      targetUpdateSkip.inc();
      log.debug("No indices have changed for module {}; skipping update", module);
      return lastInstance2modifyIndex;
    }

    targetUpdates.inc();
    this.healthyTargetsFuture = ComposableFutures.fromValue(filteredTargets);
    log.debug("{} target(s) were filtered out for module {}", newTargets.size() - filteredTargets.size(), module);
    notifyListeners(filteredTargets);

    return currentInstance2modifyIndex;
  }

  private Map<InstanceKey,Long> extractModifyIndices(final List<HealthInfoInstance> targets) {
    return targets.stream().collect(Collectors.toMap(
      InstanceKey::fromHealthInfoInstance,
      t -> t.Service.ModifyIndex));
  }

  /**
   * @return a future list of healthy targets using the provided filter. Never <code>null</code>.
   */
  public ComposableFuture<List<HealthInfoInstance>> getHealthyInstances() {
    return healthyTargetsFuture;
  }

  public void addListener(final TargetsChangedListener listener) {
    listeners.add(listener);
    healthyTargetsFuture.consume(result -> {
      if (result.isSuccess()) {
        listener.onTargetsChanged(result.getValue());
      }
    });
  }

  private void initTargetsAsync() {
    health.filterDcLocalHealthyInstances(module, envTag).consume(initialTargetsTry -> {

      Map<InstanceKey, Long> modifyIndices = Collections.emptyMap();

      if (initialTargetsTry.isSuccess()) {
        final List<HealthInfoInstance> initialTargets = nullSafeList(initialTargetsTry.getValue());

        log.debug("{} initial healthy targets fetched", initialTargets.size());
        if (initialTargets.isEmpty()) {
          log.warn("initial targets is empty for module {}", module);
        }

        modifyIndices = setTargetsIfChanged(initialTargets, Collections.emptyMap());
        initializationFuture.set(null);
      } else {
        handleTargetsFetchFailure(initialTargetsTry.getError(), false);
        initializationFuture.setException(initialTargetsTry.getError());
      }

      pollForTargetsUpdates(0, modifyIndices);
    });
  }

  private List<HealthInfoInstance> nullSafeList(final List<HealthInfoInstance> initialTargets) {
    return initialTargets == null ? Collections.<HealthInfoInstance>emptyList() : initialTargets;
  }

  private void pollForTargetsUpdates(final long fromIndex, final Map<InstanceKey, Long> lastInstance2modifyIndex) {
    final Timer.Context fetchTime = targetFetchTime.time();

    health.pollHealthyInstances(module, envTag, fromIndex).consume(aTry -> {
      fetchTime.stop();
      long nextIndex = fromIndex;
      Map<InstanceKey, Long> modifyIndices = Collections.emptyMap();
      if (aTry.isSuccess()) {
        try {
          nextIndex = extractIndex(aTry);

          if (nextIndex == fromIndex) {
            log.debug("Index unchanged. Skipping processing; index={}", fromIndex);
          } else {
            final List<HealthInfoInstance> newTargets = nullSafeList(aTry.getValue().getTypedBody());
            log.debug("{} healthy targets fetched; index={}", newTargets.size(), fromIndex);
            modifyIndices = setTargetsIfChanged(newTargets, lastInstance2modifyIndex);
          }
        } catch (final IOException | RuntimeException e) {
          handleTargetsFetchFailure(e, true);
          return;
        }
      } else if (!(aTry.getError() instanceof TimeoutException)) {
        handleTargetsFetchFailure(aTry.getError(), true);
        return;
      }

      pollForTargetsUpdates(nextIndex, modifyIndices);
    });
  }

  private void handleTargetsFetchFailure(final Throwable t, final boolean schedulRetry) {
    log.error("Failed to fetch new targets from health API: {}", t.toString());
    targetFetchErrors.inc();
    if (schedulRetry) {
      ComposableFutures.schedule(retryPoll, 2, TimeUnit.SECONDS);
    }
  }

  private Long extractIndex(final Try<TypedResponse<List<HealthInfoInstance>>> aTry) {
    try {
      return Long.valueOf(aTry.getValue().getHeader("X-Consul-Index"));
    } catch (final NumberFormatException e) {
      return 0L;
    }
  }

  public String getModule() {
    return module;
  }


  public interface TargetsChangedListener {
    void onTargetsChanged(List<HealthInfoInstance> healthTargets);
  }


  private static class InstanceKey {
    private final String node;
    private final String id;

    private InstanceKey(final String node, final String id) {
      this.node = node;
      this.id = id;
    }

    public static InstanceKey fromHealthInfoInstance(final HealthInfoInstance instance) {
      return new InstanceKey(instance.Node.Node, instance.Service.ID);
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof InstanceKey)) {
        return false;
      }

      final InstanceKey other = (InstanceKey) o;
      return Objects.equals(id, other.id) && Objects.equals(node, other.node);
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, node);
    }
  }
}
