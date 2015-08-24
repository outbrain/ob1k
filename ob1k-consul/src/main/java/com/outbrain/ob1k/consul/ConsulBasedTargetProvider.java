package com.outbrain.ob1k.consul;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.Consumer;
import com.outbrain.ob1k.concurrent.Try;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.swinfra.metrics.api.Counter;
import com.outbrain.swinfra.metrics.api.Gauge;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.outbrain.swinfra.metrics.api.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link TargetProvider} that provides targets registered in consul.
 *
 * @author Eran Harel
 */
public class ConsulBasedTargetProvider implements TargetProvider {

  private static final Logger log = LoggerFactory.getLogger(ConsulBasedTargetProvider.class);

  public final SuccessHandler<List<HealthInfoInstance>, List<String>> transformToTargets = new SuccessHandler<List<HealthInfoInstance>, List<String>>() {
    @Override
    public List<String> handle(final List<HealthInfoInstance> serviceInstances) throws ExecutionException {
      final List<String> targets = new ArrayList<>(serviceInstances.size());
      for (final HealthInfoInstance healthInfo : serviceInstances) {
        final String targetUrl = "http://" + healthInfo.Node.Node + ":" + healthInfo.Service.port("http") + healthInfo.Service.context() + urlSuffix;
        final int weight = instanceWeight(healthInfo.Service);
        for (int i = 0; i < weight; i++) {
          targets.add(targetUrl);
        }
      }

      Collections.shuffle(targets);
      targetCount = serviceInstances.size();
      return targets;
    }
  };

  private final ConsulHealth health;

  private final String module;
  private final String envTag;
  private final String urlSuffix;
  private final Map<String, Integer> tag2weight;
  private volatile int targetCount;
  private volatile List<String> targets;
  private final Timer targetFetchTime;
  private final Counter targetFetchErrors;

  private final ThreadLocal<Integer> currIndex = new ThreadLocal<Integer>() {
    @Override
    protected Integer initialValue() {
      return 0;
    }
  };

  private final Callable<?> retryPoll = new Callable<Object>() {
    @Override
    public Object call() throws Exception {
      pollForTargetsUpdates(0);
      return null;
    }
  };

  public ConsulBasedTargetProvider(final ConsulHealth health, final String module, final String envTag, final String urlSuffix, final Map<String, Integer> tag2weight, final MetricFactory metricFactory) {
    this.urlSuffix = urlSuffix == null ? "" : urlSuffix;
    this.tag2weight = tag2weight == null ? Collections.<String, Integer>emptyMap() : new HashMap<>(tag2weight);
    this.module = Preconditions.checkNotNull(module, "module must not be null");
    this.envTag = Preconditions.checkNotNull(envTag, "envTag must not be null");
    this.health = Preconditions.checkNotNull(health, "health must not be null");

    Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
    final String component = getClass().getSimpleName();

    targetFetchTime = metricFactory.createTimer(component, "targetFetchTime");
    targetFetchErrors = metricFactory.createCounter(component, "targetFetchErrors");
    metricFactory.registerGauge(component, "targetCount", new Gauge<Integer>() {
      @Override
      public Integer getValue() {
        return targetCount;
      }
    });
    targets = initTargets();

    pollForTargetsUpdates(0);
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
            targets = transformToTargets.handle(aTry.getValue().getTypedBody());
            log.debug("[{}] New targets fetched: {}", fromIndex, targets);
            nextIndex = extractIndex(aTry);
          } catch (final ExecutionException | IOException e) {
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
    log.error("Failed to fetch new targets from catalog: {}", t.toString());
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

  private List<String> initTargets() {
    try {
      return health.filterDcLocalHealthyInstances(module, envTag).continueOnSuccess(transformToTargets).get();
    } catch (InterruptedException | ExecutionException e) {
      targetFetchErrors.inc();
      throw new RuntimeException("Failed to fetch initial targets", e);
    }
  }

  @Override
  public String getTargetLogicalName() {
    return module;
  }

  @Override
  public String provideTarget() {
    if (targets.isEmpty()) {
      throw new IllegalStateException("No targets are currently registered for module " + module);
    }

    final int index = currIndex.get();
    currIndex.set(index + 1);
    final List<String> currTargets = targets;
    return currTargets.get(index % currTargets.size());
  }

  private int instanceWeight(final HealthInfoInstance.Service instance) {
    for (final Map.Entry<String, Integer> tagWeight : tag2weight.entrySet()) {
      if (instance.Tags.contains(tagWeight.getKey())) {
        return tagWeight.getValue();
      }
    }

    return 1;
  }
}
