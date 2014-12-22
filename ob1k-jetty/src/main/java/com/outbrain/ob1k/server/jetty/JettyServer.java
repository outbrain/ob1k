package com.outbrain.ob1k.server.jetty;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.servlet.DispatcherType;

import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.eclipse.jetty.jmx.MBeanContainer;
import org.eclipse.jetty.server.ConnectorStatistics;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.server.session.HashSessionIdManager;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.jetty.handler.RequestTimeoutHandler;
import com.outbrain.ob1k.server.jetty.metrics.StatisticsGuagesFactory;

/**
 * This implementation is designed to be a drop in replacement for the Tomcat container.
 *
 * @author Eran Harel
 */
public class JettyServer implements Server {

  private static final Logger log = LoggerFactory.getLogger(JettyServer.class);
  public static final int ACCEPT_QUEUE_SIZE = 128;
//  public static final int SO_LINGER_TIME = 25000; // 25 sec like in tomcat...
  public static final int DEFAULT_THREAD_IDLE_TIMEOUT = 10 * 60 * 1000; // 10 minutes

  private final MetricFactory metricFactory;
  private final org.eclipse.jetty.server.Server server;
  private final WebAppContext webAppContext;
  private final ServerConnector httpConnector;
  private final ServerConnector httpSecureConnector;
  private final String applicationName;
  private final HttpConfiguration baseHttpConfiguration;
  private final Integer maxFormSize;
  private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();

  public JettyServer(final String applicationName, final int httpPort, final SslContext sslContext, final String contextPath,
                     final int maxThreads, final Long httpConnectorIdleTimeout, final Long requestTimeoutMillis,
                     final Integer maxFormSize, final String accessLogsDirectory, final boolean compressionEnabled,
                     final String staticRootResourcesBase, final MetricFactory metricFactory) {

    System.setProperty("com.outbrain.web.context.path", contextPath);
    this.applicationName = applicationName;
    this.metricFactory = Preconditions.checkNotNull(metricFactory, "metricFactory must not be null");
    this.maxFormSize = maxFormSize;
    webAppContext = initWebAppContext(contextPath);
    server = new org.eclipse.jetty.server.Server(initThreadPool(maxThreads, metricFactory));
    log.info("Embedded Jetty server version: {}", org.eclipse.jetty.server.Server.getVersion());

    initServerLifeCycleListener();

    // TODO create a single connector with multiple ConnectionFactories - use the example from http://www.eclipse.org/jetty/documentation/current/configuring-connectors.html
    baseHttpConfiguration = initBaseHttpConfiguration();
    httpConnector = initHttpConnector(httpPort, httpConnectorIdleTimeout);
    httpSecureConnector = initHttpSecureConnector(sslContext);

    initWebHandlers(accessLogsDirectory, staticRootResourcesBase, requestTimeoutMillis);
    initCompression(compressionEnabled);

    initSessionIdManager();
    initJMX();

    // this needs to be called after all handlers are set
    initStats();

    server.setStopAtShutdown(true);
  }

  private HttpConfiguration initBaseHttpConfiguration() {
    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    httpConfiguration.addCustomizer(new ForwardedRequestCustomizer());
    httpConfiguration.setSendXPoweredBy(false);
    httpConfiguration.setSendServerVersion(false);
    return httpConfiguration;
  }

  private void initWebHandlers(final String accessLogsDirectory, final String staticRootResourcesBase, final Long requestTimeoutMillis) {
    final HandlerCollection handlers = new HandlerCollection();

    final ContextHandlerCollection contextHandler = new ContextHandlerCollection();
    final Handler[] handlersArray = staticRootResourcesBase == null ?
        new Handler[] { webAppContext } :
        new Handler[] { initStaticRootResources__Hack_Hack_Hack(staticRootResourcesBase), webAppContext };

    contextHandler.setHandlers(handlersArray);
    handlers.addHandler(contextHandler);

    final Handler accessLogHandler = initAccessLog(accessLogsDirectory);
    if (accessLogHandler != null) {
      handlers.addHandler(accessLogHandler);
    }

    server.setHandler(handlers);

    if (requestTimeoutMillis != null) {
      // must be done at the end so that the timeout handler could wrap the existing handler, otherwise timeout wont work.
      final RequestTimeoutHandler timeoutHandler = initRequestTimeoutHandler(requestTimeoutMillis);
      timeoutHandler.setHandler(server.getHandler());
      server.setHandler(timeoutHandler);
    }

  }

  private RequestTimeoutHandler initRequestTimeoutHandler(final long requestTimeoutMillis) {
    return new RequestTimeoutHandler(requestTimeoutMillis, metricFactory);
  }

  // TODO remove when OPS implement this in the infrastructure
  private Handler initStaticRootResources__Hack_Hack_Hack(final String accessLogsDirectory) {
    final ResourceHandler staticRootFilesHandler = new ResourceHandler();
    staticRootFilesHandler.setResourceBase(accessLogsDirectory);
    final ContextHandler context = new ContextHandler("/");
    context.setHandler(staticRootFilesHandler);
    return context;
  }

  private Handler initAccessLog(final String accessLogsDirectory) {
    if (null == accessLogsDirectory) {
      log.info("Access log is disabled.");
      return null;
    }

    log.info("Access log will be written to '{}' directory", accessLogsDirectory);

    final NCSARequestLog requestLog = new NCSARequestLog();
    requestLog.setFilename(accessLogsDirectory + "/access_log.yyyy_mm_dd.txt");
    requestLog.setFilenameDateFormat("yyyy_MM_dd");
    requestLog.setRetainDays(90);
    requestLog.setAppend(true);
    requestLog.setExtended(true);
    requestLog.setLogCookies(true);
    requestLog.setLogTimeZone(DateTimeZone.getDefault().getID());
    final RequestLogHandler requestLogHandler = new RequestLogHandler();
    requestLogHandler.setRequestLog(requestLog);
    return requestLogHandler;
  }

  private ThreadPool initThreadPool(final int maxThreads, final MetricFactory metricFactory) {
    final BlockingQueue<Runnable> queue = new BlockingArrayQueue<>(maxThreads, 1, maxThreads * 2);
    final QueuedThreadPool qtp = new QueuedThreadPool(maxThreads, maxThreads, DEFAULT_THREAD_IDLE_TIMEOUT, queue);
    StatisticsGuagesFactory.createQTPGauges(qtp, metricFactory);
    return qtp;
  }

  private void initCompression(final boolean compressionEnabled) {
    if (compressionEnabled) {
      final FilterHolder gzip = new FilterHolder(new GzipFilter());
      // TODO should these come from the configuration?
      gzip.setInitParameter(
          "mimeTypes",
          "text/html,text/plain,text/css,text/javascript,text/x-json,text/xml,application/json,application/javascript,application/xhtml+xml,image/svg+xml,text/zip");
      gzip.setInitParameter("minGzipSize", "1024");
      webAppContext.addFilter(gzip, "/*", EnumSet.allOf(DispatcherType.class));
    }
  }

  private void initStats() {
    initRequestStats();
    initConnectorStats(httpConnector, "http");
    initConnectorStats(httpSecureConnector, "https");
  }

  private void initConnectorStats(final ServerConnector connector, final String connectorType) {
    if (connector == null) {
      return; // ignore disabled connectors
    }

    final ConnectorStatistics stats = new ConnectorStatistics();
    connector.addBean(stats);
    StatisticsGuagesFactory.createGauges(stats, connectorType, metricFactory);
  }

  private void initRequestStats() {
    final StatisticsHandler stats = new StatisticsHandler();
    stats.setHandler(server.getHandler());
    server.setHandler(stats);

    StatisticsGuagesFactory.createGauges(stats, metricFactory);
  }

  private ServerConnector initHttpSecureConnector(final SslContext sslContext) {
    if (null == sslContext) {
      log.warn("SSL wasn't configured and will be disabled.");
      return null;
    }

    final SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setKeyStorePath(sslContext.getKeyStorePath());
    sslContextFactory.setKeyStorePassword(sslContext.getKeyStorePassword());
    sslContextFactory.setKeyManagerPassword(sslContext.getKeyManagerPassword());
    sslContextFactory.setProtocol("TLS");
    sslContextFactory.setNeedClientAuth(false);

    final HttpConfiguration httpsConfig = new HttpConfiguration(baseHttpConfiguration);
    httpsConfig.addCustomizer(new SecureRequestCustomizer());

    final ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"),
        new HttpConnectionFactory(httpsConfig));
    sslConnector.setPort(sslContext.getSecurePort());
    sslConnector.setAcceptQueueSize(ACCEPT_QUEUE_SIZE);
//    sslConnector.setSoLingerTime(SO_LINGER_TIME);

    server.addConnector(sslConnector);

    return sslConnector;
  }

  private ServerConnector initHttpConnector(final int port, final Long idleTimeout) {
    log.info("httpPort=[{}]", port);

    final ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(baseHttpConfiguration));
    connector.setPort(port);
    connector.setAcceptQueueSize(ACCEPT_QUEUE_SIZE);
//    connector.setSoLingerTime(SO_LINGER_TIME);
    if (idleTimeout != null) {
      connector.setIdleTimeout(idleTimeout);
    }

    server.addConnector(connector);
    return connector;
  }

  private void initSessionIdManager() {
    final HashSessionIdManager sessionIdManager = new HashSessionIdManager();
    sessionIdManager.setRandom(new Random());
    server.setSessionIdManager(sessionIdManager);
  }

  private void initJMX() {
    final MBeanContainer mbContainer = new MBeanContainer(ManagementFactory.getPlatformMBeanServer());
    server.addBean(mbContainer);
  }

  private WebAppContext initWebAppContext(final String contextPath) {
    log.info("contextPath=[{}]", contextPath);
    final String warPath = System.getProperty("com.outbrain.application.war.path", "src/main/webapp/");
    log.info("warPath=[{}]", warPath);
    final WebAppContext wac = new WebAppContext();
    wac.setWar(warPath);
    wac.setContextPath(contextPath);
    wac.setCompactPath(true);
    wac.setThrowUnavailableOnStartupException(true);

    if (maxFormSize != null) {
      wac.setMaxFormContentSize(maxFormSize);
    }

    // in production this is set by theforce.sh
    // in dev jetty will use the defaults (/tmp/...)
    final String tmpDirPath = System.getProperty("com.outbrain.application.webappcontext.tempdir");
    if (tmpDirPath != null) {
      wac.setTempDirectory(new File(tmpDirPath));
    }

    return wac;
  }

  @Override
  public InetSocketAddress start() {
    log.info("################## Starting OB1K Jetty server for module '{}' ##################", applicationName);
    try {
      server.start();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to start Jetty server", e);
    }

    onStarted();
    return new InetSocketAddress(httpConnector.getLocalPort());
  }

  public InetSocketAddress getSecuredAddress() {
    if (!server.isStarted()) {
      throw new IllegalStateException("Server was not started, I don't have an address yet");
    }

    return new InetSocketAddress(httpSecureConnector.getLocalPort());
  }

  @Override
  public void stop() {
    log.info("################## Stopping OB1K Jetty server for module '{}' ##################", applicationName);
    try {
      server.stop();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to stop Jetty server", e);
    }
  }

  @Override
  public String getContextPath() {
    return webAppContext.getContextPath();
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void onStarted() {
    log.info("**************** Module '{}' Started ****************", applicationName);
    for (final Listener listener : listeners) {
      listener.serverStarted(this);
    }
  }

  private void initServerLifeCycleListener() {
    // adding better logging to understand failures in shutdown process.
    server.addLifeCycleListener(new AbstractLifeCycle.AbstractLifeCycleListener() {
      @Override
      public void lifeCycleFailure(final LifeCycle event, final Throwable cause) {
        if (cause instanceof MultiException) {
          final MultiException multi = (MultiException) cause;
          final List<Throwable> reasons = multi.getThrowables();
          for (final Throwable reason : reasons) {
            log.warn("errors while shutting down", reason);
          }
        } else {
          log.warn("errors while shutting down", cause);
        }
      }
    });
  }

}
