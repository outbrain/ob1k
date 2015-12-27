package com.outbrain.ob1k.server.build;

import com.google.common.collect.Sets;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.server.BeanContext;
import com.outbrain.ob1k.server.Server;
import com.outbrain.ob1k.server.StaticPathResolver;
import com.outbrain.ob1k.server.netty.NettyServer;
import com.outbrain.ob1k.server.registry.ServiceRegistry;
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 2:16 PM
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public class ServerBuilder implements InitialPhase, ChoosePortPhase, ChooseContextPathPhase, ChooseServiceCreationTypePhase,
                                      AddServiceFromContextPhase, AddRawServicePhase, StaticResourcesPhase, ExtraParamsPhase
{
  public static final int DEFAULT_MAX_CONTENT_LENGTH = 256 * 1024;
  
  private int port = 0;
  private final ServiceRegistry registry;
  private final RequestMarshallerRegistry marshallerRegistry;
  private final List<String> staticFolders;
  private final Map<String, String> staticResources;
  private String contextPath = "";
  private String appName = "";
  private final Map<String, String> staticMappings;
  private boolean acceptKeepAlive = false;
  private boolean supportZip = true;
  private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;
  private long requestTimeoutMs = -1;

  private com.outbrain.swinfra.metrics.api.MetricFactory metricFactory;

  private ThreadPoolConfig threadPoolConfig = null;
  private BeanContext ctx;
  private final List<ServiceDescriptor> serviceDescriptors = new ArrayList<>();
  private final List<Server.Listener> listeners = new LinkedList<>();

  public static InitialPhase newBuilder() {
    return new ServerBuilder();
  }

  private ServerBuilder() {
    this.staticFolders = new ArrayList<>();
    this.staticMappings = new HashMap<>();
    this.staticResources = new HashMap<>();
    this.marshallerRegistry = new RequestMarshallerRegistry();
    this.registry = new ServiceRegistry();
  }

  @Override
  public ChooseContextPathPhase setPort(final int port) {
    this.port = port;
    return this;
  }

  @Override
  public ChooseContextPathPhase useRandomPort() {
    this.port = 0;
    return this;
  }

  @Override
  public ChooseContextPathPhase configurePorts(final PortsProvider provider) {
    provider.configure(this);
    return this;
  }

  private static class ThreadPoolConfig {
    public final int minSize;
    public final int maxSize;

    private ThreadPoolConfig(final int minSize, final int maxSize) {
      this.minSize = minSize;
      this.maxSize = maxSize;
    }
  }

  private static Executor createExecutorService(final ThreadPoolConfig config, final SyncRequestQueueObserver queueObserver) {
    final int queueCapacity = config.maxSize * 2; // TODO make this configurable ?
    final BlockingQueue<Runnable> requestQueue =
            new ObservableBlockingQueue<>(new LinkedBlockingQueue<Runnable>(queueCapacity), queueObserver);

    return new ThreadPoolExecutor(config.minSize, config.maxSize, 30, TimeUnit.SECONDS, requestQueue,
        new DefaultThreadFactory("syncReqPool"), new ThreadPoolExecutor.AbortPolicy());
  }

  @Override
  public ServerBuilder configureExecutorService(final int minSize, final int maxSize) {
    this.threadPoolConfig = new ThreadPoolConfig(minSize, maxSize);
    return this;
  }

  @Override
  public ExtraParamsPhase setMetricFactory(final MetricFactory metricFactory) {
    this.metricFactory = metricFactory;
    return this;
  }

  @Override
  public ExtraParamsPhase addListener(final Server.Listener listener) {
    this.listeners.add(listener);
    return this;
  }

  @Override
  public AddRawServicePhase addService(final Service service, final String path, final ServiceFilter... filters) {
    return addService(service, path, true, filters);
  }

  private static class ServiceDescriptor {
    final String name;
    final Service service;
    final List<AsyncFilter> asyncFilters;
    final List<SyncFilter> syncFilters;
    final List<StreamFilter> streamFilters;
    final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointsBinding;
    final boolean bindPrefix;

    private ServiceDescriptor(final String name, final Service service, final List<AsyncFilter> asyncFilters,
                              final List<SyncFilter> syncFilters, final List<StreamFilter> streamFilters,
                              final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointsBinding,
                              final boolean bindPrefix) {
      this.name = name;
      this.service = service;
      this.asyncFilters = asyncFilters;
      this.syncFilters = syncFilters;
      this.streamFilters = streamFilters;
      this.endpointsBinding = endpointsBinding;
      this.bindPrefix = bindPrefix;
    }
  }

  @Override
  public AddRawServicePhase addService(final Service service, final String path, final boolean bindPrefix, final ServiceFilter... filters) {
    final List<AsyncFilter> asyncFilters = new ArrayList<>();
    final List<SyncFilter> syncFilters = new ArrayList<>();
    final List<StreamFilter> streamFilters = new ArrayList<>();

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

    serviceDescriptors.add(new ServiceDescriptor(path, service, asyncFilters, syncFilters, streamFilters, null, bindPrefix));
    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase configureExtraParams(final ExtraParamsProvider provider) {
    provider.configureExtraParams(this);
    return this;
  }

  @Override
  public ExtraParamsPhase acceptKeepAlive(final boolean keepAlive) {
    this.acceptKeepAlive = keepAlive;
    return this;
  }

  @Override
  public ExtraParamsPhase supportZip(final boolean useZip) {
    this.supportZip = useZip;
    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase withServicesFrom(final BeanContext ctx, final ContextBasedServiceProvider provider) {
    this.ctx = ctx;
    provider.addServices(this, ctx);
    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase withServices(final RawServiceProvider provider) {
    provider.addServices(this);
    return this;
  }

  @Override
  public AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType, final String path) {
    return addServiceFromContext(ctxName, serviceType, null, path);
  }

  @Override
  public final AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                                final Class<? extends ServiceFilter> filterType, final String path)
  {
    return addServiceFromContext(ctxName, serviceType, filterType, path, true);
  }

  @Override
  public AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                          final Class<? extends ServiceFilter> filterType,
                                                          final String path, final boolean bindPrefix)
  {
    final ArrayList<Class<? extends ServiceFilter>> filterTypes = new ArrayList<>();
    if (filterType != null) {
      filterTypes.add(filterType);
    }

    return addServiceFromContext(ctxName, serviceType, filterTypes, path, bindPrefix);
  }

  @Override
  public AddServiceFromContextPhase addServices(final ContextBasedServiceProvider provider) {
    provider.addServices(this, ctx);
    return this;
  }

  @Override
  public AddRawServicePhase addServices(final RawServiceProvider provider) {
    provider.addServices(this);
    return this;
  }


  @Override
  public AddRawServicePhase addServices(final RegistryServiceProvider provider, final String path, final ServiceFilter... filters) {
    provider.addServices(this, registry, path, filters);
    return this;
  }

  @Override
  public final AddServiceFromContextPhase addServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                                final List<Class<? extends ServiceFilter>> filterTypes,
                                                                final String path, final boolean bindPrefix) {
    final List<ServiceFilter> filters = new ArrayList<>();
    if (filterTypes != null) {
      for (final Class<? extends ServiceFilter> filterType : filterTypes) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }
    }
    final Service service = ctx.getBean(ctxName, serviceType);

    addService(service, path, bindPrefix, filters.toArray(new ServiceFilter[filters.size()]));
    return this;
  }

  private ServiceBuilder defineService(final Service service, final String path) {
    return defineService(service, path, true);
  }

  private ServiceBuilder defineService(final Service service, final String path, final boolean bindPrefix) {
    return new ServiceBuilder(service, path, bindPrefix);
  }

  @Override
  public AddRawServicePhase defineService(final Service service, final String path, final ServiceBindingProvider provider) {
    final ServiceBuilder builder = defineService(service, path);
    provider.configureService(builder);
    builder.addService();
    return this;
  }

  @Override
  public AddRawServicePhase defineService(final Service service, final String path, final boolean bindPrefix, final ServiceBindingProvider provider) {
    final ServiceBuilder builder = defineService(service, path, bindPrefix);
    provider.configureService(builder);
    builder.addService();
    return this;
  }

  @Override
  public AddServiceFromContextPhase defineServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                             final String path, final ContextBasedServiceBindingProvider provider) {
    return defineServiceFromContext(ctxName, serviceType, path, true, provider);
  }

  @Override
  public AddServiceFromContextPhase defineServiceFromContext(final String ctxName, final Class<? extends Service> serviceType,
                                                             final String path, final boolean bindPrefix, final ContextBasedServiceBindingProvider provider) {
    final Service service = ctx.getBean(ctxName, serviceType);
    final ServiceBuilder builder = new ServiceBuilder(service, path, bindPrefix);
    provider.configureService(builder);
    builder.addService();
    return this;
  }

  @Override
  public ServerBuilder addStaticPath(final String path) {
    if (!staticFolders.contains(path)) {
      this.staticFolders.add(path);
    }

    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase configureStaticResources(final StaticResourcesProvider provider) {
    provider.configureResources(this);
    return this;
  }

  @Override
  public ServerBuilder addStaticResource(final String mapping, final String location) {
    addStaticPath(location);
    this.staticResources.put(mapping, location);
    return this;
  }

  @Override
  public ServerBuilder addStaticMapping(final String virtualPath, final String realPath) {
    staticMappings.put(virtualPath, realPath);
    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase setContextPath(final String contextPath, final String appName) {
    this.contextPath = contextPath;
    this.appName = appName;
    this.registry.setContextPath(contextPath);
    return this;
  }

  @Override
  public ChooseServiceCreationTypePhase setContextPath(final String contextPath) {
    return setContextPath(contextPath, extractAppName(contextPath));
  }

  private static String extractAppName(final String contextPath) {
    if (contextPath.startsWith("/"))
      return contextPath.substring(1);

    return contextPath;
  }

  @Override
  public ExtraParamsPhase setMaxContentLength(final int maxContentLength) {
    this.maxContentLength = maxContentLength;
    return this;
  }

  @Override
  public ExtraParamsPhase setRequestTimeout(final long timeout, final TimeUnit unit) {
    this.requestTimeoutMs = unit.toMillis(timeout);
    return this;
  }

  private static void registerServices(final List<ServiceDescriptor> serviceDescriptors, final ServiceRegistry registry,
                                       final Executor executorService) {
    for (final ServiceDescriptor desc: serviceDescriptors) {
      if (desc.endpointsBinding != null) {
        registry.register(desc.name, desc.service, desc.endpointsBinding, desc.bindPrefix, executorService);
      } else {
        registry.register(desc.name, desc.service,
            desc.asyncFilters, desc.syncFilters, desc.streamFilters,
            desc.bindPrefix, executorService);
      }
    }
  }

  @Override
  public Server build() {
    final ChannelGroup activeChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    final SyncRequestQueueObserver queueObserver = new SyncRequestQueueObserver(activeChannels, metricFactory);
    Executor executorService = null;
    if (threadPoolConfig != null) {
      executorService = createExecutorService(threadPoolConfig, queueObserver);
    }

    registerServices(serviceDescriptors, registry, executorService);
    final StaticPathResolver staticResolver = new StaticPathResolver(contextPath, Sets.newHashSet(staticFolders), staticMappings, staticResources);

    final NettyServer server = new NettyServer(port, registry, marshallerRegistry, staticResolver, queueObserver, activeChannels, contextPath,
            appName, acceptKeepAlive, supportZip, metricFactory, maxContentLength, requestTimeoutMs);
    server.addListeners(listeners);
    return server;
  }

  public class ServiceBuilder implements ContextBasedServiceBuilderPhase, RawServiceBuilderPhase {
    public final Service service;
    public final String name;
    public final boolean bindPrefix;
    private final Map<String, Map<HttpRequestMethodType, ServiceRegistry.EndpointDescriptor>> endpointsBinding;

    public ServiceBuilder(final Service service, final String name, final boolean bindPrefix) {
      this.service = service;
      this.name = name;
      this.bindPrefix = bindPrefix;
      this.endpointsBinding = new HashMap<>();
    }

    public ServerBuilder addService() {
      serviceDescriptors.add(new ServiceDescriptor(name, service,
          null, null, null, endpointsBinding, bindPrefix));

      return ServerBuilder.this;
    }

    @Override
    public ServiceBuilder addEndpoint(final String methodName, final String path, final ServiceFilter... filters) {
      return addEndpoint(HttpRequestMethodType.ANY, methodName, path, filters);
    }

    @Override
    public ServiceBuilder addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final ServiceFilter... filters) {
      final List<ServiceFilter> filtersList = filters != null ? Arrays.asList(filters) : null;
      return addEndpoint(requestMethodType, methodName, path, filtersList);
    }

    private ServiceBuilder addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final List<ServiceFilter> filters) {
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

      endpointDescriptors.put(requestMethodType, new ServiceRegistry.EndpointDescriptor(method, filters, requestMethodType));

      return this;
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path) {
      return addEndpoint(HttpRequestMethodType.ANY, methodName, path);
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path) {
      return addEndpoint(requestMethodType, methodName, path, new ServiceFilter[0]);
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName, final Class<? extends ServiceFilter> filterType) {
      return addEndpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filterType);
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                                       final Class<? extends ServiceFilter> filterType) {
      final ServiceFilter filter = ctx.getBean(ctxName, filterType);
      return addEndpoint(requestMethodType, methodName, path, filter);
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName,
                                                       final List<Class<? extends ServiceFilter>> filtersType) {
      return addEndpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filtersType);
    }

    @Override
    public ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                                       final List<Class<? extends ServiceFilter>> filtersType) {
      final List<ServiceFilter> filters = new ArrayList<>();
      for (final Class<? extends ServiceFilter> filterType : filtersType) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }

      return addEndpoint(requestMethodType, methodName, path, filters);
    }
  }

}
