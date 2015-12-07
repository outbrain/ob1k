package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.server.Server;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.TimeUnit;

public class DefaultConfigureBuilder<E extends ExtendableServerBuilder<E>> extends BuilderSection<E> {

  private final ServerBuilderState state;

  public DefaultConfigureBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
  }

  public DefaultConfigureBuilder<E> usePort(final int port) {
    state.setPort(port);
    return this;
  }

  public DefaultConfigureBuilder<E> useRandomPort() {
    return usePort(0);
  }

  public DefaultConfigureBuilder<E> maxContentLength(final int maxContentLength) {
    state.setMaxContentLength(maxContentLength);
    return this;
  }

  public DefaultConfigureBuilder<E> requestTimeout(final long timeout, final TimeUnit unit) {
    state.setRequestTimeoutMs(unit.toMillis(timeout));
    return this;
  }

  public DefaultConfigureBuilder<E> useMetricFactory(final MetricFactory metricFactory) {
    state.setMetricFactory(metricFactory);
    return this;
  }

  public DefaultConfigureBuilder<E> acceptKeepAlive(final boolean keepAlive) {
    state.setAcceptKeepAlive(keepAlive);
    return this;
  }

  public DefaultConfigureBuilder<E> supportZip(final boolean useZip) {
    state.setSupportZip(useZip);
    return this;
  }

  public DefaultConfigureBuilder<E> configureExecutorService(final int minSize, final int maxSize) {
    state.setThreadPoolMinSize(minSize);
    state.setThreadPoolMaxSize(maxSize);
    return this;
  }

  public DefaultConfigureBuilder<E> addListener(final Server.Listener listener) {
    state.addListener(listener);
    return this;
  }
}
