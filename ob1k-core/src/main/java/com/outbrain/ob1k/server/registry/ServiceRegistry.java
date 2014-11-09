package com.outbrain.ob1k.server.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.concurrent.ComposableExecutorService;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.common.marshalling.TypeHelper;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.server.MethodParamNamesExtractor;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.server.registry.endpoints.AbstractServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.AsyncServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.StreamServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.SyncServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 11:25 AM
 */
public class ServiceRegistry {
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

  private final PathTrie<AbstractServerEndpoint> endpoints;
  private String contextPath;
  private final RequestMarshallerRegistry marshallerRegistry;

  public ServiceRegistry(final RequestMarshallerRegistry marshallerRegistry) {
    this.endpoints = new PathTrie<>();
    this.marshallerRegistry = marshallerRegistry;
  }

  public void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  public AbstractServerEndpoint findEndpoint(final String path, final Map<String, String> pathParams) {
    return endpoints.retrieve(path, pathParams);
  }

  public void register(final String name, final Service service, final boolean bindPrefix,
                       final ComposableExecutorService executorService) {
    register(name, service, null, null, null, bindPrefix, executorService);
  }

  public static class EndpointDescriptor {
    public final Method method;
    public final List<? extends ServiceFilter> filters;

    public EndpointDescriptor(Method method, List<? extends ServiceFilter> filters) {
      this.method = method;
      this.filters = filters;
    }
  }

  public void register(final String name, final Service service, final Map<String, EndpointDescriptor> descriptors,
                       final boolean bindPrefix, final ComposableExecutorService executorService) {

    if (contextPath == null) {
      throw new RuntimeException("can't add service before context path is set.");
    }

    final Map<Method, List<String>> methodsParams;
    try {
      methodsParams = MethodParamNamesExtractor.extract(service.getClass(), getMethods(descriptors));
    } catch (final Exception e) {
      throw new RuntimeException("service " + name +" can't be analyzed", e);
    }

    for (final String methodBind: descriptors.keySet()) {
      final StringBuilder path = new StringBuilder();
      path.append(contextPath);
      if (!contextPath.endsWith("/")) {
        path.append("/");
      }

      if (name.startsWith("/")) {
        path.append(name.substring(1));
      } else {
        path.append(name);
      }

      if (!name.endsWith("/")) {
        path.append("/");
      }

      if (methodBind.startsWith("/")) {
        path.append(methodBind.substring(1));
      } else {
        path.append(methodBind);
      }

      final EndpointDescriptor endpointDesc = descriptors.get(methodBind);
      final Method method = endpointDesc.method;
      final String[] params = methodsParams.get(method).toArray(new String[methodsParams.get(method).size()]);
      final AbstractServerEndpoint endpoint = isAsyncMethod(method) ?
          new AsyncServerEndpoint(service, getAsyncFilters(endpointDesc.filters, methodBind), method, params) :
          isStreamingMethod(method) ?
              new StreamServerEndpoint(service, getStreamFilters(endpointDesc.filters, methodBind), method, params) :
              new SyncServerEndpoint(service, getSyncFilters(endpointDesc.filters, methodBind), method, params, executorService);

      endpoints.insert(path.toString(), endpoint, bindPrefix);
    }
  }

  private static List<Method> getMethods(final Map<String, EndpointDescriptor> descriptors) {
    final List<Method> methods = new ArrayList<>();
    for (EndpointDescriptor desc : descriptors.values()) {
      methods.add(desc.method);
    }

    return methods;
  }

  private static AsyncFilter[] getAsyncFilters(List<? extends ServiceFilter> filters, String methodName) {
    if (filters == null)
      return null;

    final AsyncFilter[] result = new AsyncFilter[filters.size()];
    int index = 0;
    for (ServiceFilter filter : filters) {
      if (filter instanceof AsyncFilter) {
        result[index++] = (AsyncFilter) filter;
      } else {
        throw new RuntimeException("method " + methodName + " can only receive async filters");
      }
    }

    return result;
  }

  private static SyncFilter[] getSyncFilters(List<? extends ServiceFilter> filters, String methodName) {
    if (filters == null)
      return null;

    final SyncFilter[] result = new SyncFilter[filters.size()];
    int index = 0;
    for (ServiceFilter filter : filters) {
      if (filter instanceof SyncFilter) {
        result[index++] = (SyncFilter) filter;
      } else {
        throw new RuntimeException("method " + methodName + " can only receive sync filters");
      }
    }

    return result;
  }

  private static StreamFilter[] getStreamFilters(List<? extends ServiceFilter> filters, String methodName) {
    if (filters == null)
      return null;

    final StreamFilter[] result = new StreamFilter[filters.size()];
    int index = 0;
    for (ServiceFilter filter : filters) {
      if (filter instanceof StreamFilter) {
        result[index++] = (StreamFilter) filter;
      } else {
        throw new RuntimeException("method " + methodName + " can only receive stream filters");
      }
    }

    return result;
  }


  public void register(final String name, final Service service, final List<AsyncFilter> asyncFilters,
                       final List<SyncFilter> syncFilters, final List<StreamFilter> streamFilters, final boolean bindPrefix,
                       final ComposableExecutorService executorService) {

    final Map<String, EndpointDescriptor> descriptors = getEndpointsDescriptor(service,
        executorService, asyncFilters, syncFilters, streamFilters);
    register(name, service, descriptors, bindPrefix, executorService);
  }

  private Map<String, EndpointDescriptor> getEndpointsDescriptor(final Service service,
                                                                final ComposableExecutorService executorService,
                                                                final List<AsyncFilter> asyncFilters,
                                                                final List<SyncFilter> syncFilters,
                                                                final List<StreamFilter> streamFilters) {

    final Method[] methods = service.getClass().getDeclaredMethods();
    final Map<String, EndpointDescriptor> result = new HashMap<>();
    for (final Method m : methods) {
      final int modifiers = m.getModifiers();
      if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
        if (isAsyncMethod(m)) {
          result.put(m.getName(), new EndpointDescriptor(m, asyncFilters));
          marshallerRegistry.registerTypes(TypeHelper.extractTypes(m));
        } else if (isStreamingMethod(m)) {
          result.put(m.getName(), new EndpointDescriptor(m, streamFilters));
          marshallerRegistry.registerTypes(TypeHelper.extractTypes(m));
        } else if (executorService != null) {
          // a sync method with a defined executor service.
          result.put(m.getName(), new EndpointDescriptor(m, syncFilters));
          marshallerRegistry.registerTypes(TypeHelper.extractTypes(m));
        } else {
          logger.info("method " + m.getName() + " wasn't bounded. sync method needs a configured executor service");
        }
      }
    }

    // in case of a single method service (e.g. SelfTestService) we don't want to include the method name as a path
    if (result.size() == 1) {
      result.put("", result.values().iterator().next());
    }

    return result;
  }

  public SortedMap<String, AbstractServerEndpoint> getRegisteredEndpoints() {
    return endpoints.getPathToValueMapping();
  }

  public void logRegisteredEndpoints() {
    for (final Map.Entry<String, AbstractServerEndpoint> endpoint : getRegisteredEndpoints().entrySet()) {
      final AbstractServerEndpoint endpointValue = endpoint.getValue();
      logger.info("Registered endpoint [{} ==> {}]", endpoint.getKey(), endpointValue.getTargetAsString());
    }
  }

  private boolean isAsyncMethod(final Method m) {
    return m.getReturnType() == ComposableFuture.class;
  }

  private boolean isStreamingMethod(final Method m) {
    return m.getReturnType() == Observable.class;
  }

}
