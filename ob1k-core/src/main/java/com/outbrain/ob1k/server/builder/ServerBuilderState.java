package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.swinfra.metrics.api.MetricFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * state of the Server Builder.
 * This interface is used to expose the inner state of the builder to specific section builders
 * or extension builders but hide it from users of the Builder.
 */
public interface ServerBuilderState {

  void setPort(final int port);

  void setContextPath(final String contextPath);

  void setAppName(final String appName);

  void setAcceptKeepAlive(final boolean acceptKeepAlive);

  void setSupportZip(final boolean supportZip);

  void setMaxContentLength(final int maxContentLength);

  void setRequestTimeoutMs(final long requestTimeoutMs);

  void setThreadPoolMinSize(final int threadPoolMinSize);

  void setThreadPoolMaxSize(final int threadPoolMaxSize);

  void setMetricFactory(final MetricFactory metricFactory);

  void addListener(final Server.Listener listener);

  void addStaticFolder(final String folder);

  void addStaticMapping(final String virtualPath, final String realPath);

  void addStaticResource(final String mapping, final String location);

  void addServiceDescriptor(final Service service, final String path, final ServiceFilter... filters);

  void removeFiltersFromLastServiceDescriptor(Class<? extends ServiceFilter> filter);

  void setBindPrefixToLastDescriptor(boolean bindPrefix);

  void setEndpointBinding(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter[] filters);

  ServiceRegistryView getRegistry();

  boolean alreadyRegisteredServices();

  int getPort();

  String getContextPath();

  String getAppName();

  boolean isAcceptKeepAlive();

  boolean isSupportZip();

  int getMaxContentLength();

  long getRequestTimeoutMs();

  int getThreadPoolMinSize();

  int getThreadPoolMaxSize();

  MetricFactory getMetricFactory();

  List<Server.Listener> getListeners();

  Set<String> getStaticFolders();

  Map<String, String> getStaticResources();

  Map<String, String> getStaticMappings();
}
