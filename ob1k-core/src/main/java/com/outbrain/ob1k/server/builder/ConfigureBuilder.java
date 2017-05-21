package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.server.Server;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.concurrent.TimeUnit;

/**
 * portion of the ServerBuilder methods used for configuration.
 *
 * ports, timeouts, listeners etc.
 *
 *
 * @param <B> ability to extend this builder
 */
public class ConfigureBuilder<B extends ConfigureBuilder<B>> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface ConfigureBuilderSection extends BuilderSection<ConfigureBuilder> {}

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

  public B idleTimeout(final long timeout, final TimeUnit unit) {
    state.setIdleTimeoutMs(unit.toMillis(timeout));
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
