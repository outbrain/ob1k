package com.outbrain.ob1k.server.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.HttpRequestMethodType;
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

  private final PathTrie<Map<HttpRequestMethodType, AbstractServerEndpoint>> endpoints;
  private String contextPath;
  private final RequestMarshallerRegistry marshallerRegistry;

  public ServiceRegistry(final RequestMarshallerRegistry marshallerRegistry) {
    this.endpoints = new PathTrie<>();
    this.marshallerRegistry = marshallerRegistry;
  }

  public void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  public AbstractServerEndpoint findEndpoint(final String path, final HttpRequestMethodType requestMethodType, final Map<String, String> pathParams) {
    final Map<HttpRequestMethodType, AbstractServerEndpoint> serviceEndpoints = endpoints.retrieve(path, pathParams);
    if (serviceEndpoints == null) {
      return null;
    }
    if (serviceEndpoints.containsKey(HttpRequestMethodType.ANY)) {
      return serviceEndpoints.get(HttpRequestMethodType.ANY);
    }
    return serviceEndpoints.get(requestMethodType);
  }

  public void register(final String name, final Service service, final boolean bindPrefix,
                       final ComposableExecutorService executorService) {
    register(name, service, null, null, null, bindPrefix, executorService);
  }

  public static class EndpointDescriptor {
    public final Method method;
    public final List<? extends ServiceFilter> filters;
    public final HttpRequestMethodType requestMethodType;

    public EndpointDescriptor(final Method method, final List<? extends ServiceFilter> filters, final HttpRequestMethodType requestMethodType) {
      this.method = method;
      this.filters = filters;
      this.requestMethodType = requestMethodType;
    }
  }

  public void register(final String name, final Service service, final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> descriptors,
                       final boolean bindPrefix, final ComposableExecutorService executorService) {

    if (contextPath == null) {
      throw new RuntimeException("Can't add service before context path is set.");
    }

    final Map<Method, List<String>> methodsParams;

    try {
      methodsParams = MethodParamNamesExtractor.extract(service.getClass(), getMethods(descriptors));
    } catch (final Exception e) {
      throw new RuntimeException("Service " + name + " can't be analyzed", e);
    }

    /**
     * Building full path
     */
    for (final String methodBind: descriptors.keySet()) {

      final StringBuilder path = new StringBuilder();

      path.append(contextPath);

      if (!contextPath.endsWith("/")) {
        path.append('/');
      }

      if (name.startsWith("/")) {
        path.append(name.substring(1));
      } else {
        path.append(name);
      }

      if (!name.endsWith("/")) {
        path.append('/');
      }

      if (methodBind.startsWith("/")) {
        path.append(methodBind.substring(1));
      } else {
        path.append(methodBind);
      }

      final Map<HttpRequestMethodType, EndpointDescriptor> endpointDescriptors = descriptors.get(methodBind);
      final Map<HttpRequestMethodType, AbstractServerEndpoint> endpointsMap = new HashMap<>();
      
      if (endpointDescriptors.containsKey(HttpRequestMethodType.ANY) && endpointDescriptors.size() > 1) {
        throw new RuntimeException("Cannot add more request methods for the path after defining an ANY (all) endpoint path");
      }

      for (final Map.Entry<HttpRequestMethodType, EndpointDescriptor> endpointDescriptorEntry : endpointDescriptors.entrySet()) {
        final EndpointDescriptor endpointDesc = endpointDescriptorEntry.getValue();
        final Method method = endpointDesc.method;
        final List<String> methodParamNames = methodsParams.get(method);

        marshallerRegistry.registerTypes(TypeHelper.extractTypes(method));

        validatePathParam(methodBind, endpointDesc, method, methodParamNames);

        final String[] params = methodParamNames.toArray(new String[methodParamNames.size()]);
        final AbstractServerEndpoint endpoint = isAsyncMethod(method) ?
                new AsyncServerEndpoint(service, getAsyncFilters(endpointDesc.filters, methodBind), method, endpointDesc.requestMethodType, params) :
                isStreamingMethod(method) ?
                        new StreamServerEndpoint(service, getStreamFilters(endpointDesc.filters, methodBind), method, endpointDesc.requestMethodType, params) :
                        new SyncServerEndpoint(service, getSyncFilters(endpointDesc.filters, methodBind), method, endpointDesc.requestMethodType, params, executorService);
        endpointsMap.put(endpointDescriptorEntry.getKey(), endpoint);
      }

      endpoints.insert(path.toString(), endpointsMap, bindPrefix);
    }
  }

  private void validatePathParam(final String methodBind, final EndpointDescriptor endpointDesc, final Method method, final List<String> methodParamNames) {
    if (methodBind.contains("{")) {
      int index = methodBind.indexOf('{');
      int methodParamPos = 0;

      while (index >= 0) {
        final int endIndex = methodBind.indexOf('}', index);
        final String pathParameter = methodBind.substring(index + 1, endIndex);

        if (!methodParamNames.contains(pathParameter)) {
          throw new RuntimeException("Parameter " + pathParameter + " does not exists in method signature");
        }

        if (endpointDesc.requestMethodType == HttpRequestMethodType.POST || endpointDesc.requestMethodType == HttpRequestMethodType.PUT) {
          if (methodParamNames.indexOf(pathParameter) != methodParamPos) {
            throw new RuntimeException("Path parameters should be in prefix when using method binding, i.e. methodName(id, name) => {id}/{name} or {id} with body of [name]");
          }
          methodParamPos++;
        }

        final Class paramType = method.getParameterTypes()[methodParamNames.indexOf(pathParameter)];

        if (!paramType.isPrimitive() && !String.class.isAssignableFrom(paramType)) {
          throw new RuntimeException("Path parameter " + paramType + " can be only primitive or String type");
        }

        index = methodBind.indexOf('{', endIndex);
      }
    }
  }

  private static List<Method> getMethods(final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> descriptors) {
    final List<Method> methods = new ArrayList<>();
    for (final Map<HttpRequestMethodType, EndpointDescriptor> descMap : descriptors.values()) {
      for (final EndpointDescriptor desc : descMap.values()) {
        methods.add(desc.method);
      }
    }

    return methods;
  }

  private static AsyncFilter[] getAsyncFilters(final List<? extends ServiceFilter> filters, final String methodName) {
    if (filters == null)
      return null;

    final AsyncFilter[] result = new AsyncFilter[filters.size()];
    int index = 0;
    for (final ServiceFilter filter : filters) {
      if (filter instanceof AsyncFilter) {
        result[index++] = (AsyncFilter) filter;
      } else {
        throw new RuntimeException("method " + methodName + " can only receive async filters");
      }
    }

    return result;
  }

  private static SyncFilter[] getSyncFilters(final List<? extends ServiceFilter> filters, final String methodName) {
    if (filters == null)
      return null;

    final SyncFilter[] result = new SyncFilter[filters.size()];
    int index = 0;
    for (final ServiceFilter filter : filters) {
      if (filter instanceof SyncFilter) {
        result[index++] = (SyncFilter) filter;
      } else {
        throw new RuntimeException("method " + methodName + " can only receive sync filters");
      }
    }

    return result;
  }

  private static StreamFilter[] getStreamFilters(final List<? extends ServiceFilter> filters, final String methodName) {
    if (filters == null)
      return null;

    final StreamFilter[] result = new StreamFilter[filters.size()];
    int index = 0;
    for (final ServiceFilter filter : filters) {
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

    final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> descriptors = getEndpointsDescriptor(service,
            executorService, asyncFilters, syncFilters, streamFilters);
    register(name, service, descriptors, bindPrefix, executorService);
  }

  private Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> getEndpointsDescriptor(final Service service,
                                                                final ComposableExecutorService executorService,
                                                                final List<AsyncFilter> asyncFilters,
                                                                final List<SyncFilter> syncFilters,
                                                                final List<StreamFilter> streamFilters) {

    final Method[] methods = service.getClass().getDeclaredMethods();
    final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> result = new HashMap<>();

    for (final Method m : methods) {
      final int modifiers = m.getModifiers();
      if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
        if (isAsyncMethod(m)) {
          result.put(m.getName(), singleEndpointDescToMap(new EndpointDescriptor(m, asyncFilters, HttpRequestMethodType.ANY)));
        } else if (isStreamingMethod(m)) {
          result.put(m.getName(), singleEndpointDescToMap(new EndpointDescriptor(m, streamFilters, HttpRequestMethodType.ANY)));
        } else if (executorService != null) {
          // A sync method with a defined executor service.
          result.put(m.getName(), singleEndpointDescToMap(new EndpointDescriptor(m, syncFilters, HttpRequestMethodType.ANY)));
        } else {
          logger.info("Method " + m.getName() + " wasn't bounded. Sync method needs a configured executor service");
        }
      }
    }

    // in case of a single method service (e.g. SelfTestService) we don't want to include the method name as a path
    if (result.size() == 1) {
      result.put("", result.values().iterator().next());
    }

    return result;
  }

  private Map<HttpRequestMethodType, EndpointDescriptor> singleEndpointDescToMap(final EndpointDescriptor endpointDescriptor) {
    final Map<HttpRequestMethodType, EndpointDescriptor> endpointDescriptorMap = Maps.newHashMap();
    endpointDescriptorMap.put(HttpRequestMethodType.ANY, endpointDescriptor);
    return endpointDescriptorMap;
  }

  public SortedMap<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> getRegisteredEndpoints() {
    return endpoints.getPathToValueMapping();
  }

  public void logRegisteredEndpoints() {
    for (final Map.Entry<String, Map<HttpRequestMethodType, AbstractServerEndpoint>> pathEndpointsMap : getRegisteredEndpoints().entrySet()) {
      for (final Map.Entry<HttpRequestMethodType, AbstractServerEndpoint> endpointEntry : pathEndpointsMap.getValue().entrySet()) {
        final AbstractServerEndpoint endpointValue = endpointEntry.getValue();
        logger.info("Registered endpoint [{} ==> {}, via method: {}]", pathEndpointsMap.getKey(), endpointValue.getTargetAsString(), endpointValue.requestMethodType);
      }
    }
  }

  private boolean isAsyncMethod(final Method m) {
    return m.getReturnType() == ComposableFuture.class;
  }

  private boolean isStreamingMethod(final Method m) {
    return m.getReturnType() == Observable.class;
  }

}
