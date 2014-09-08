package com.outbrain.ob1k.server.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

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

  public void register(final String name, final Service service, final AsyncFilter[] asyncFilters,
                       final SyncFilter[] syncFilters, final StreamFilter[] streamFilters,
                       final Map<String, Method> methodBinds, final boolean bindPrefix,
                       final ComposableExecutorService executorService) {

    if (contextPath == null) {
      throw new RuntimeException("can't add service before context path is set.");
    }

    final Map<Method, String[]> methodsParams;
    try {
      methodsParams = MethodParamNamesExtractor.extract(service.getClass(), methodBinds.values());
    } catch (final Exception e) {
      throw new RuntimeException("service " + name +" can't be analyzed", e);
    }

    for (final String methodBind: methodBinds.keySet()) {
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

      final Method method = methodBinds.get(methodBind);
      final String[] params = methodsParams.get(method);
      final AbstractServerEndpoint endpoint = isAsyncMethod(method) ?
          new AsyncServerEndpoint(service, asyncFilters, method, params) :
          isStreamingMethod(method) ?
              new StreamServerEndpoint(service, streamFilters, method, params) :
              new SyncServerEndpoint(service, syncFilters, method, params, executorService);
      endpoints.insert(path.toString(), endpoint, bindPrefix);
    }
  }

  public void register(final String name, final Service service, final AsyncFilter[] asyncFilters,
                       final SyncFilter[] syncFilters, final StreamFilter[] streamFilters, final boolean bindPrefix,
                       final ComposableExecutorService executorService) {

    register(name, service, asyncFilters, syncFilters, streamFilters, getEndpointMappings(service,
             executorService), bindPrefix, executorService);
  }

  private Map<String, Method> getEndpointMappings(final Service service, final ComposableExecutorService executorService) {
    final Method[] methods = service.getClass().getDeclaredMethods();
    final Map<String, Method> result = new HashMap<>();
    for (final Method m : methods) {
      final int modifiers = m.getModifiers();
      if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
        if (isAsyncMethod(m) || isStreamingMethod(m) || executorService != null) {
          result.put(m.getName(), m);
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
