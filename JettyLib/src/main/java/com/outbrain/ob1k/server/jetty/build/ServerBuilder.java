package com.outbrain.ob1k.server.jetty.build;

import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.jetty.JettyServer;
import com.outbrain.ob1k.server.jetty.SslContext;
import com.outbrain.swinfra.metrics.api.MetricFactory;

/**
 * Created by aronen on 9/8/14.
 */
public class ServerBuilder {

  protected String contextPath;
  protected int port;
  protected Integer securePort;
  protected SslContextBuilder sslContext;
  protected int maxThreads = 200;
  protected boolean accessLogEnabled = false;
  protected boolean compressionEnabled = true;
  protected Long httpConnectorIdleTimeout = null;
  protected String staticRootResourcesBase = null;
  protected Long requestTimeoutMillis;
  protected MetricFactory metricFactory;
  protected String applicationName;
  protected String logDirectory;

  public ServerBuilder configurationPorts(final int httpPort, final int securePort) {
    this.port = httpPort;
    this.securePort = securePort;

    return this;
  }

  public ServerBuilder setPort(final int port) {
    this.port = port;
    return this;
  }

  public ServerBuilder setSecurePort(final int securePort) {
    this.securePort = securePort;
    return this;
  }

  public ServerBuilder setContextPath(final String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  public ServerBuilder setMaxThreads(final int maxThreads) {
    this.maxThreads = maxThreads;
    return this;
  }

  /**
   * Sets the request hard timeout in milliseconds. Disabled by default.
   * When this is set, each request that takes longer than this timeout will get interrupted.
   *
   * @param requestTimeoutMillis request timeout in milliseconds
   */
  public ServerBuilder setRequestTimeout(final long requestTimeoutMillis) {
    this.requestTimeoutMillis = requestTimeoutMillis;
    return this;
  }

  public ServerBuilder enableAccessLog() {
    this.accessLogEnabled = true;
    return this;
  }

  public SslContextBuilder configureSsl(final String keyStorePath, final String keyStorePassword, final String keyManagerPassword) {
    this.sslContext = new SslContextBuilder()
        .keyStorePath(keyStorePath)
        .keyStorePassword(keyStorePassword)
        .keyManagerPassword(keyManagerPassword);
    return sslContext;
  }

  public ServerBuilder setMetricFactory(final MetricFactory metricFactory) {
    this.metricFactory = metricFactory;

    return this;
  }

  public ServerBuilder setApplicationName(final String applicationName) {
    this.applicationName = applicationName;

    return this;
  }

  public ServerBuilder setLogDirectory(final String logDirectory) {
    this.logDirectory = logDirectory;
    return this;
  }

  public Server build() {
    final String accessLogsDirectory = accessLogEnabled ? logDirectory : null;
    return new JettyServer(applicationName, port, sslContext, contextPath, maxThreads, httpConnectorIdleTimeout,
        requestTimeoutMillis, accessLogsDirectory, compressionEnabled, staticRootResourcesBase, metricFactory);
  }


  /**
   * Used to configure SSL connectors
   *
   * @author Eran Harel
   */
  public class SslContextBuilder implements SslContext {
    private String keyStorePath;
    private String keyStorePassword;
    private String keyManagerPassword;

    public SslContextBuilder keyStorePath(final String keyStorePath) {
      this.keyStorePath = keyStorePath;
      return this;
    }

    public SslContextBuilder keyStorePassword(final String keyStorePassword) {
      this.keyStorePassword = keyStorePassword;
      return this;
    }

    public SslContextBuilder keyManagerPassword(final String keyManagerPassword) {
      this.keyManagerPassword = keyManagerPassword;
      return this;
    }

    public ServerBuilder enableSsl() {
      return ServerBuilder.this;
    }

    @Override
    public int getSecurePort() {
      return securePort == null ? 0 : securePort;
    }

    public String getKeyManagerPassword() {
      return keyManagerPassword;
    }

    public String getKeyStorePassword() {
      return keyStorePassword;
    }

    public String getKeyStorePath() {
      return keyStorePath;
    }
  }

}
