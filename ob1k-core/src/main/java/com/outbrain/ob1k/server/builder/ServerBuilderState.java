package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.http.common.ContentType;
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

  void setPort(int port);

  void setContextPath(String contextPath);

  void setAppName(String appName);

  void setAcceptKeepAlive(boolean acceptKeepAlive);

  void setSupportZip(boolean supportZip);

  void setMaxContentLength(int maxContentLength);

  void setIdleTimeoutMs(long idleTimeoutMs);

  void setRequestTimeoutMs(long requestTimeoutMs);

  void setThreadPoolMinSize(int threadPoolMinSize);

  void setThreadPoolMaxSize(int threadPoolMaxSize);

  void setMetricFactory(MetricFactory metricFactory);

  void addListener(Server.Listener listener);

  void addStaticFolder(String folder);

  void addStaticMapping(String virtualPath, String realPath);

  void addStaticResource(String mapping, String location);

  void addServiceDescriptor(Service service, String path, ServiceFilter... filters);

  void removeFiltersFromLastServiceDescriptor(Class<? extends ServiceFilter> filter);

  void setBindPrefixToLastDescriptor(boolean bindPrefix);

  void setFiltersToLastDescriptor(ServiceFilter... filters);

  void setEndpointBinding(HttpRequestMethodType methodType, String methodName, String path, ServiceFilter[] filters);

  void setMarshallerRegistry(RequestMarshallerRegistry marshallers);

  ServiceRegistryView getRegistry();

  boolean alreadyRegisteredServices();

  int getPort();

  String getContextPath();

  String getAppName();

  boolean isAcceptKeepAlive();

  boolean isSupportZip();

  int getMaxContentLength();

  long getRequestTimeoutMs();

  long getIdleTimeoutMs();

  int getThreadPoolMinSize();

  int getThreadPoolMaxSize();

  MetricFactory getMetricFactory();

  List<Server.Listener> getListeners();

  Set<String> getStaticFolders();

  Map<String, String> getStaticResources();

  Map<String, String> getStaticMappings();
}
