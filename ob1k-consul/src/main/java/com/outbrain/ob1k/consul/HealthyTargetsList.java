package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.consul.filter.AllTargetsPredicate;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Maintains a list of healthy targets based on consul registration.
 * The updated list can be retrieved using a {@link TargetsChangedListener}
 * provided to the {@link #addListener(TargetsChangedListener)} method, or by calling {@link #getHealthyInstances()}.
 *
 * @author Eran Harel
 */
public class HealthyTargetsList {

  private static final Logger log = LoggerFactory.getLogger(HealthyTargetsList.class);

  private volatile List<HealthInfoInstance> healthyTargets;
  private final List<TargetsChangedListener> listeners = new CopyOnWriteArrayList<>();

  private final String module;
  private final String envTag;
  private final Predicate<HealthInfoInstance> targetsPredicate;

  private final ConsulHealth health;

  private final Timer targetFetchTime;
  private final Counter targetFetchErrors;

  private final Callable<?> retryPoll = new Callable<Object>() {
    @Override
    public Object call() throws Exception {
      pollForTargetsUpdates(0);
      return null;
    }
  };

  public HealthyTargetsList(final ConsulHealth health, final String module, final String envTag, final Predicate<HealthInfoInstance> targetsPredicate, final MetricFactory metricFactory) {
    this.health = Preconditions.checkNotNull(health, "health must not be null");
    this.module = Preconditions.checkNotNull(module, "module must not be null");
    this.envTag = Preconditions.checkNotNull(envTag, "envTag must not be null");
    this.targetsPredicate = (targetsPredicate == null ? AllTargetsPredicate.INSTANCE : targetsPredicate);

    Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
    final String component = getClass().getSimpleName();

    targetFetchTime = metricFactory.createTimer(component, "targetFetchTime");
    targetFetchErrors = metricFactory.createCounter(component, "targetFetchErrors");
    metricFactory.registerGauge(component, "targetCount", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return healthyTargets == null ? 0 : healthyTargets.size();
      }
    });

    setTargets(initTargets());
    pollForTargetsUpdates(0);
  }

  private void notifyListeners() {
    for (final TargetsChangedListener listener : listeners) {
      listener.onTargetsChanged(healthyTargets);
    }
  }

  private void setTargets(final List<HealthInfoInstance> newTargets){
    final List<HealthInfoInstance> filteredTargets = ImmutableList.copyOf(Iterables.filter(newTargets, targetsPredicate));
    this.healthyTargets = filteredTargets;

    log.debug("{} target(s) were filtered out", newTargets.size() - filteredTargets.size());
    notifyListeners();
  }

  public List<HealthInfoInstance> getHealthyInstances() {
    return healthyTargets;
  }

  public void addListener(final TargetsChangedListener listener) {
    listeners.add(listener);
    if(null != healthyTargets) {
      listener.onTargetsChanged(healthyTargets);
    }
  }

  private List<HealthInfoInstance> initTargets() {
    try {
      return health.filterDcLocalHealthyInstances(module, envTag).get();
    } catch (InterruptedException | ExecutionException e) {
      targetFetchErrors.inc();
      throw new RuntimeException("Failed to fetch initial targets", e);
    }
  }


  private void pollForTargetsUpdates(final long fromIndex) {
    final Timer.Context fetchTime = targetFetchTime.time();

    health.pollHealthyInstances(module, envTag, fromIndex).consume(new Consumer<TypedResponse<List<HealthInfoInstance>>>() {
      @Override
      public void consume(final Try<TypedResponse<List<HealthInfoInstance>>> aTry) {
        fetchTime.stop();
        long nextIndex = 0;
        if (aTry.isSuccess()) {
          try {
            final List<HealthInfoInstance> newTargets = aTry.getValue().getTypedBody();
            log.debug("{} healthy targets fetched; index={}", newTargets.size(), fromIndex);
            setTargets(newTargets);
            nextIndex = extractIndex(aTry);
          } catch (final IOException e) {
            handleTargetsFetchFailure(e);
            return;
          }
        } else if (!(aTry.getError() instanceof TimeoutException)) {
          handleTargetsFetchFailure(aTry.getError());
          return;
        }

        pollForTargetsUpdates(nextIndex);
      }
    });
  }

  private void handleTargetsFetchFailure(final Throwable t) {
    log.error("Failed to fetch new targets from health API: {}", t.toString());
    targetFetchErrors.inc();
    ComposableFutures.schedule(retryPoll, 2, TimeUnit.SECONDS);
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
}
