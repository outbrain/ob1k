package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.StaticPathResolver;
import com.outbrain.ob1k.server.netty.NettyServer;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
import com.outbrain.ob1k.server.registry.ServiceRegistryView;
import com.outbrain.ob1k.server.util.ObservableBlockingQueue;
import com.outbrain.ob1k.server.util.SyncRequestQueueObserver;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class ExtendableServerBuilder<E extends ExtendableServerBuilder<E>> {

  public static final int DEFAULT_MAX_CONTENT_LENGTH = 256 * 1024;

  private int port = 0;
  private String contextPath = "";
  private String appName = "";
  private boolean acceptKeepAlive = false;
  private boolean supportZip = true;
  private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
  private long requestTimeoutMs = -1;
  private int threadPoolMinSize;
  private int threadPoolMaxSize;
  private MetricFactory metricFactory;
  private final List<Server.Listener> listeners = new LinkedList<>();
  private final Deque<ServiceDescriptor> serviceDescriptors = new LinkedList<>();
  private final Set<String> staticFolders = new HashSet<>();
  private final Map<String, String> staticResources = new HashMap<>();
  private final Map<String, String> staticMappings = new HashMap<>();

  private final ServiceRegistry registry;
  private final RequestMarshallerRegistry marshallerRegistry;

  protected ExtendableServerBuilder() {
    this.marshallerRegistry = new RequestMarshallerRegistry();
    this.registry = new ServiceRegistry(marshallerRegistry);
  }

  public E and(final BuilderProvider<ServerBuilderState> extensionBuilder) {
    extensionBuilder.provide(innerState());
    return self();
  }

  protected abstract E self();

  public final Server build() {
    final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    final SyncRequestQueueObserver queueObserver = new SyncRequestQueueObserver(activeChannels, metricFactory);
    final Executor executorService = (threadPoolMaxSize > 0 && threadPoolMinSize > 0) ? createExecutorService(queueObserver) : null;
    registerServices(serviceDescriptors, registry, executorService);
    final StaticPathResolver staticResolver = new StaticPathResolver(contextPath, staticFolders, staticMappings, staticResources);

    final NettyServer server = new NettyServer(port, registry, marshallerRegistry, staticResolver, queueObserver, activeChannels, contextPath,
            appName, acceptKeepAlive, supportZip, metricFactory, maxContentLength, requestTimeoutMs);
    server.addListeners(listeners);
    return server;
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
      final List<SyncFilter> syncBucket = new ArrayList<>();
      final List<StreamFilter> streamBucket = new ArrayList<>();

      sortFiltersTo(asyncBucket, syncBucket, streamBucket, filters);

      serviceDescriptors.add(new ServiceDescriptor(path, service, asyncBucket, syncBucket, streamBucket, null, true));
    }

    @Override
    public void setBindPrefixToLastDescriptor(final boolean bindPrefix) {
      serviceDescriptors.getLast().setBindPrefix(bindPrefix);
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
        if (Modifier.isPublic(m.getModifiers()) && !Modifier.isStatic(m.getModifiers())) {
          if (m.getName().equals(methodName)) {
            method = m;
            break;
          }
        }
      }

      if (method == null) {
        throw new RuntimeException("Method: " + methodName + " was not found or is not a proper service method");
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
    public ServiceRegistryView getRegistry() {
      return registry;
    }
  }

  //////////////////////////////

  private static class ServiceDescriptor {
    private final String name;
    private final Service service;
    private final List<AsyncFilter> asyncFilters;
    private final List<SyncFilter> syncFilters;
    private final List<StreamFilter> streamFilters;
    private Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointBinding;
    private boolean bindPrefix;

    private ServiceDescriptor(final String name, final Service service, final List<AsyncFilter> asyncFilters,
                              final List<SyncFilter> syncFilters, final List<StreamFilter> streamFilters,
                              final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointBinding,
                              final boolean bindPrefix) {
      this.name = name;
      this.service = service;
      this.asyncFilters = asyncFilters;
      this.syncFilters = syncFilters;
      this.streamFilters = streamFilters;
      this.endpointBinding = endpointBinding;
      this.bindPrefix = bindPrefix;
    }

    public void setBindPrefix(final boolean bindPrefix) {
      this.bindPrefix = bindPrefix;
    }

    public void setEndpointBinding(final Map<String,Map<HttpRequestMethodType,ServiceRegistry.EndpointDescriptor>> endpointBinding) {
      this.endpointBinding = endpointBinding;
    }
  }

  private static void registerServices(final Deque<ServiceDescriptor> serviceDescriptors, final ServiceRegistry registry,
                                       final Executor executorService) {
    for (final ServiceDescriptor desc: serviceDescriptors) {
      if (desc.endpointBinding != null) {
        registry.register(desc.name, desc.service, desc.endpointBinding, desc.bindPrefix, executorService);
      } else {
        registry.register(desc.name, desc.service,
            desc.asyncFilters, desc.syncFilters, desc.streamFilters,
            desc.bindPrefix, executorService);
      }
    }
  }

  private Executor createExecutorService(final SyncRequestQueueObserver queueObserver) {
    final int queueCapacity = threadPoolMaxSize * 2; // TODO make this configurable ?
    final BlockingQueue<Runnable> requestQueue =
            new ObservableBlockingQueue<>(new LinkedBlockingQueue<Runnable>(queueCapacity), queueObserver);

    return new ThreadPoolExecutor(threadPoolMinSize, threadPoolMaxSize, 30, TimeUnit.SECONDS, requestQueue,
            new DefaultThreadFactory("syncReqPool"), new ThreadPoolExecutor.AbortPolicy());
  }

  private void sortFiltersTo(final List<AsyncFilter> asyncFilters,
                             final List<SyncFilter> syncFilters,
                             final List<StreamFilter> streamFilters,
                             final ServiceFilter[] filters) {
    if (filters != null) {
      for (final ServiceFilter filter: filters) {
        if (filter instanceof SyncFilter) {
          syncFilters.add((SyncFilter) filter);
        }

        if (filter instanceof AsyncFilter) {
          asyncFilters.add((AsyncFilter) filter);
        }

        if (filter instanceof StreamFilter) {
          streamFilters.add((StreamFilter) filter);
        }
      }
    }
  }
}
