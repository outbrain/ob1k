package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.server.Server;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.TimeUnit;

public class ConfigureBuilder<B extends ConfigureBuilder<B>> {

  private final ServerBuilderState state;

  public ConfigureBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public B usePort(final int port) {
    state.setPort(port);
    return self();
  }

  public B useRandomPort() {
    return usePort(0);
  }

  public B maxContentLength(final int maxContentLength) {
    state.setMaxContentLength(maxContentLength);
    return self();
  }

  public B requestTimeout(final long timeout, final TimeUnit unit) {
    state.setRequestTimeoutMs(unit.toMillis(timeout));
    return self();
  }

  public B useMetricFactory(final MetricFactory metricFactory) {
    state.setMetricFactory(metricFactory);
    return self();
  }

  public B acceptKeepAlive(final boolean keepAlive) {
    state.setAcceptKeepAlive(keepAlive);
    return self();
  }

  public B supportZip(final boolean useZip) {
    state.setSupportZip(useZip);
    return self();
  }

  public B configureExecutorService(final int minSize, final int maxSize) {
    state.setThreadPoolMinSize(minSize);
    state.setThreadPoolMaxSize(maxSize);
    return self();
  }

  public B addListener(final Server.Listener listener) {
    state.addListener(listener);
    return self();
  }

  @SuppressWarnings("unchecked")
  private B self() {
    return (B) this;
  }
}
