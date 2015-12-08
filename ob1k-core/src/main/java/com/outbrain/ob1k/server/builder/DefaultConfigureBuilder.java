package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.server.Server;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.TimeUnit;

public class DefaultConfigureBuilder {

  private final ServerBuilderState state;

  public DefaultConfigureBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public DefaultConfigureBuilder usePort(final int port) {
    state.setPort(port);
    return this;
  }

  public DefaultConfigureBuilder useRandomPort() {
    return usePort(0);
  }

  public DefaultConfigureBuilder maxContentLength(final int maxContentLength) {
    state.setMaxContentLength(maxContentLength);
    return this;
  }

  public DefaultConfigureBuilder requestTimeout(final long timeout, final TimeUnit unit) {
    state.setRequestTimeoutMs(unit.toMillis(timeout));
    return this;
  }

  public DefaultConfigureBuilder useMetricFactory(final MetricFactory metricFactory) {
    state.setMetricFactory(metricFactory);
    return this;
  }

  public DefaultConfigureBuilder acceptKeepAlive(final boolean keepAlive) {
    state.setAcceptKeepAlive(keepAlive);
    return this;
  }

  public DefaultConfigureBuilder supportZip(final boolean useZip) {
    state.setSupportZip(useZip);
    return this;
  }

  public DefaultConfigureBuilder configureExecutorService(final int minSize, final int maxSize) {
    state.setThreadPoolMinSize(minSize);
    state.setThreadPoolMaxSize(maxSize);
    return this;
  }

  public DefaultConfigureBuilder addListener(final Server.Listener listener) {
    state.addListener(listener);
    return this;
  }
}
