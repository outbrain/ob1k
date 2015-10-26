package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.eager.ComposablePromise;
import com.outbrain.ob1k.consul.filter.AllTargetsPredicate;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
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

  private final Callable<?> retryPoll = new Callable<Object>() {
    @Override
    public Object call() throws Exception {
      pollForTargetsUpdates(0);
      return null;
    }
  };

  private final ComposablePromise<?> initializationFuture = ComposableFutures.newPromise(false);

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
        return getHealthyInstancesCount();
      }
    });

    initTargetsAsync();
    pollForTargetsUpdates(0);
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

  private void setTargets(final List<HealthInfoInstance> newTargets){
    final List<HealthInfoInstance> filteredTargets = ImmutableList.copyOf(Iterables.filter(newTargets, targetsPredicate));
    this.healthyTargetsFuture = ComposableFutures.fromValue(filteredTargets);

    log.debug("{} target(s) were filtered out", newTargets.size() - filteredTargets.size());
    notifyListeners(filteredTargets);
  }

  /**
   * @return a future list of healthy targets using the provided filter. Never <code>null</code>.
   */
  public ComposableFuture<List<HealthInfoInstance>> getHealthyInstances() {
    return healthyTargetsFuture;
  }

  public void addListener(final TargetsChangedListener listener) {
    listeners.add(listener);
    healthyTargetsFuture.consume(new Consumer<List<HealthInfoInstance>>() {
      @Override
      public void consume(final Try<List<HealthInfoInstance>> result) {
        if (result.isSuccess()) {
          listener.onTargetsChanged(result.getValue());
        }
      }
    });
  }

  private void initTargetsAsync() {
    health.filterDcLocalHealthyInstances(module, envTag).consume(new Consumer<List<HealthInfoInstance>>() {
      @Override
      public void consume(final Try<List<HealthInfoInstance>> initialTargetsTry) {
        if (initialTargetsTry.isSuccess()) {
          log.debug("{} initial healthy targets fetched", initialTargetsTry.getValue().size());
          setTargets(initialTargetsTry.getValue());
          initializationFuture.set(null);
        } else {
          handleTargetsFetchFailure(initialTargetsTry.getError(), false);
          initializationFuture.setException(initialTargetsTry.getError());
        }
      }
    });
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
            handleTargetsFetchFailure(e, true);
            return;
          }
        } else if (!(aTry.getError() instanceof TimeoutException)) {
          handleTargetsFetchFailure(aTry.getError(), true);
          return;
        }

        pollForTargetsUpdates(nextIndex);
      }
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
}
