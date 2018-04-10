package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.StaticPathResolver;
import com.outbrain.ob1k.server.netty.NettyServer;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isAsyncMethod;
import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isEndpoint;
import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isStreamingMethod;
import static java.util.Collections.unmodifiableList;

/**
 * An abstract class for every concrete ob1k server builder
 *
 * This class keep the state and implements the build()
 * method but allows the user to define its own builder with its own API.
 *
 * See ServerBuilder for simple example of a concrete implementation.
 * or com.outbrain.ob1k.server.spring.SpringContextServerBuilder for another example of a concrete builder
 * implementation that (once moved outside core) will allow us not to have a dependency on spring (not even Provided).
 *
 */
public abstract class AbstractServerBuilder {

  public static final int DEFAULT_MAX_CONTENT_LENGTH = 256 * 1024;

  private int port = 0;
  private String contextPath = "";
  private String appName = "";
  private boolean acceptKeepAlive = false;
  private boolean supportZip = true;
  private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
  private long requestTimeoutMs = -1;
  private long idleTimeoutMs = 60_000;
  private int threadPoolMinSize;
  private int threadPoolMaxSize;
  private MetricFactory metricFactory;
  private final List<Server.Listener> listeners = new LinkedList<>();
  private final Deque<ServiceDescriptor> serviceDescriptors = new LinkedList<>();
  private final Set<String> staticFolders = new HashSet<>();
  private final Map<String, String> staticResources = new HashMap<>();
  private final Map<String, String> staticMappings = new HashMap<>();

  private final ServiceRegistry registry;

  protected AbstractServerBuilder() {
    this.registry = new ServiceRegistry();
  }

  public final Server build() {
    registerAllServices();

    final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    final StaticPathResolver staticResolver = new StaticPathResolver(contextPath, staticFolders, staticMappings, staticResources);

    final NettyServer server = new NettyServer(port, registry, staticResolver,  activeChannels, contextPath,
            appName, acceptKeepAlive, idleTimeoutMs, supportZip, metricFactory, maxContentLength, requestTimeoutMs);
    server.addListeners(listeners);

    return server;
  }

  protected void registerAllServices() {
    registerServices(serviceDescriptors, registry);
  }

  protected ServiceRegistry getServiceRegistry() {
    return registry;
  }

  protected RequestMarshallerRegistry getMarshallerRegistry() {
    return registry.getMarshallerRegistry();
  }

  protected ServerBuilderState innerState() {
    return new InnerState();
  }

  private class InnerState implements ServerBuilderState {

    @Override
    public void setPort(final int portToUse) {
      port = portToUse;
    }

    @Override
    public void setContextPath(final String contextPathToUse) {
      contextPath = contextPathToUse;
      registry.setContextPath(contextPathToUse);
    }

    @Override
    public void setAppName(final String appNameToUse) {
      appName = appNameToUse;
    }

    @Override
    public void setAcceptKeepAlive(final boolean acceptKeepAliveToUse) {
      acceptKeepAlive = acceptKeepAliveToUse;
    }

    @Override
    public void setIdleTimeoutMs(final long idleTimeout) {
      idleTimeoutMs = idleTimeout;
    }

    @Override
    public void setSupportZip(final boolean supportZipToUse) {
      supportZip = supportZipToUse;
    }

    @Override
    public void setMaxContentLength(final int maxContentLengthToUse) {
      maxContentLength = maxContentLengthToUse;
    }

    @Override
    public void setRequestTimeoutMs(final long requestTimeoutMsToUse) {
      requestTimeoutMs = requestTimeoutMsToUse;
    }

    @Override
    public void setThreadPoolMinSize(final int threadPoolMinSizeToUse) {
      threadPoolMinSize = threadPoolMinSizeToUse;
    }

    @Override
    public void setThreadPoolMaxSize(final int threadPoolMaxSizeToUse) {
      threadPoolMaxSize = threadPoolMaxSizeToUse;
    }

    @Override
    public void setMetricFactory(final MetricFactory metricFactoryToUse) {
      metricFactory = metricFactoryToUse;
    }

    @Override
    public void addListener(final Server.Listener listener) {
      listeners.add(listener);
    }

    @Override
    public void addStaticFolder(final String folder) {
      staticFolders.add(folder);
    }

    @Override
    public void addStaticMapping(final String virtualPath, final String realPath) {
      staticMappings.put(virtualPath, realPath);
    }

    @Override
    public void addStaticResource(final String mapping, final String location) {
      addStaticFolder(location);
      staticResources.put(mapping, location);
    }

    @Override
    public void addServiceDescriptor(final Service service, final String path, final ServiceFilter... filters) {
      final List<AsyncFilter> asyncBucket = new ArrayList<>();
      final List<StreamFilter> streamBucket = new ArrayList<>();

      sortFiltersTo(asyncBucket, streamBucket, filters);

      serviceDescriptors.add(new ServiceDescriptor(path, service, asyncBucket, streamBucket, null, true));
    }

    @Override
    public void removeFiltersFromLastServiceDescriptor(final Class<? extends ServiceFilter> filterClass) {
      final ServiceDescriptor descriptor = serviceDescriptors.getLast();

      if (AsyncFilter.class.isAssignableFrom(filterClass)) {
        removeFiltersOfClass(filterClass, descriptor.asyncFilters);
      }

      if (StreamFilter.class.isAssignableFrom(filterClass)) {
        removeFiltersOfClass(filterClass, descriptor.streamFilters);
      }
    }

    @Override
    public void setBindPrefixToLastDescriptor(final boolean bindPrefix) {
      serviceDescriptors.getLast().setBindPrefix(bindPrefix);
    }

    @Override
    public void setFiltersToLastDescriptor(final ServiceFilter... filters) {
      serviceDescriptors.getLast().addFilters(filters);
    }

    @Override
    public void setEndpointBinding(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter[] filters) {
      final ServiceDescriptor descriptor = serviceDescriptors.getLast();
      final Service service = descriptor.service;
      final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointsBinding;
      if (descriptor.endpointBinding == null) {
        endpointsBinding = new HashMap<>();
        descriptor.setEndpointBinding(endpointsBinding);
      } else {
        endpointsBinding = descriptor.endpointBinding;
      }

      final Method[] methods = service.getClass().getDeclaredMethods();
      Method method = null;
      for (final Method m : methods) {
        if (isEndpoint(m)) {
          if (m.getName().equals(methodName)) {
            if (!isAsyncMethod(m) && !isStreamingMethod(m)) {
              throw new IllegalArgumentException("Method: " + methodName + " does not return ComposableFuture or Observable");
            }
            method = m;
            break;
          }
        }
      }

      if (method == null) {
        throw new IllegalArgumentException("Method: " + methodName + " was not found or is not a proper service method");
      }

      final Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor> endpointDescriptors;

      if (endpointsBinding.containsKey(path)) {
        endpointDescriptors = endpointsBinding.get(path);
      } else {
        endpointDescriptors = new HashMap<>();
        endpointsBinding.put(path, endpointDescriptors);
      }

      endpointDescriptors.put(methodType, new ServiceRegistry.EndpointDescriptor(method, Arrays.asList(filters), methodType));
    }

    @Override
    public void setMarshallerRegistry(final RequestMarshallerRegistry marshallerRegistry) {
      registry.setMarshallerRegistry(marshallerRegistry);
    }

    @Override
    public ServiceRegistryView getRegistry() {
      return registry;
    }

    @Override
    public boolean alreadyRegisteredServices() {
      return !serviceDescriptors.isEmpty();
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public String getContextPath() {
      return contextPath;
    }

    @Override
    public String getAppName() {
      return appName;
    }

    @Override
    public boolean isAcceptKeepAlive() {
      return acceptKeepAlive;
    }

    @Override
    public boolean isSupportZip() {
      return supportZip;
    }

    @Override
    public int getMaxContentLength() {
      return maxContentLength;
    }

    @Override
    public long getRequestTimeoutMs() {
      return requestTimeoutMs;
    }

    @Override
    public long getIdleTimeoutMs() {
      return idleTimeoutMs;
    }

    @Override
    public int getThreadPoolMinSize() {
      return threadPoolMinSize;
    }

    @Override
    public int getThreadPoolMaxSize() {
      return threadPoolMaxSize;
    }

    @Override
    public MetricFactory getMetricFactory() {
      return metricFactory;
    }

    @Override
    public List<Server.Listener> getListeners() {
      return unmodifiableList(listeners);
    }

    @Override
    public Set<String> getStaticFolders() {
      return Collections.unmodifiableSet(staticFolders);
    }

    @Override
    public Map<String, String> getStaticResources() {
      return Collections.unmodifiableMap(staticResources);
    }

    @Override
    public Map<String, String> getStaticMappings() {
      return Collections.unmodifiableMap(staticMappings);
    }

    private <T extends ServiceFilter> void removeFiltersOfClass(final Class<? extends ServiceFilter> filterClass, final List<T> filterList) {
      final List<T> toRemove = new LinkedList<>();
      for (final T candidate : filterList) {
        if (candidate.getClass().equals(filterClass)) {
          toRemove.add(candidate);
        }
      }
      for (final T filter : toRemove) {
        filterList.remove(filter);
      }
    }

  }

  //////////////////////////////

  private static class ServiceDescriptor {
    private final String name;
    private final Service service;
    private final List<AsyncFilter> asyncFilters;
    private final List<StreamFilter> streamFilters;
    private Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointBinding;
    private boolean bindPrefix;

    private ServiceDescriptor(final String name, final Service service, final List<AsyncFilter> asyncFilters,
                              final List<StreamFilter> streamFilters,
                              final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointBinding,
                              final boolean bindPrefix) {
      this.name = name;
      this.service = service;
      this.asyncFilters = asyncFilters;
      this.streamFilters = streamFilters;
      this.endpointBinding = endpointBinding;
      this.bindPrefix = bindPrefix;
    }

    public void setBindPrefix(final boolean bindPrefix) {
      this.bindPrefix = bindPrefix;
    }

    public void setEndpointBinding(final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointBinding) {
      this.endpointBinding = endpointBinding;
    }

    public void addFilters(final ServiceFilter... filters) {
      sortFiltersTo(asyncFilters, streamFilters, filters);
    }
  }

  private static void registerServices(final Deque<ServiceDescriptor> serviceDescriptors, final ServiceRegistry registry) {
    for (final ServiceDescriptor desc: serviceDescriptors) {
      if (desc.endpointBinding != null) {
        registry.registerEndpoints(desc.endpointBinding, desc.name, desc.service,
                desc.asyncFilters, desc.streamFilters, desc.bindPrefix);
      } else {
        registry.register(desc.name, desc.service,
            desc.asyncFilters, desc.streamFilters,
            desc.bindPrefix);
      }
    }
  }

  private static void sortFiltersTo(final List<AsyncFilter> asyncFilters,
                             final List<StreamFilter> streamFilters,
                             final ServiceFilter[] filters) {
    if (filters != null) {
      for (final ServiceFilter filter: filters) {
        if (filter instanceof AsyncFilter && !contains(filter, asyncFilters)) {
          asyncFilters.add((AsyncFilter) filter);
        }

        if (filter instanceof StreamFilter && !contains(filter, streamFilters)) {
          streamFilters.add((StreamFilter) filter);
        }
      }
    }
  }

  private static boolean contains(final ServiceFilter filter, final List<? extends ServiceFilter> filters) {
    for (final ServiceFilter inList : filters) {
      if (filter.getClass().equals(inList.getClass())) {
        return true;
      }
    }
    return false;
  }
}
