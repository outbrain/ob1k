package com.outbrain.ob1k.server.registry;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.common.marshalling.TypeHelper;
import com.outbrain.ob1k.server.MethodParamNamesExtractor;
import com.outbrain.ob1k.server.registry.endpoints.AsyncServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpoint;
import com.outbrain.ob1k.server.registry.endpoints.ServerEndpointView;
import com.outbrain.ob1k.server.registry.endpoints.StreamServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isAsyncMethod;
import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isEndpoint;
import static com.outbrain.ob1k.common.endpoints.ServiceEndpointContract.isStreamingMethod;
import static java.util.Collections.unmodifiableSortedMap;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 11:25 AM
 */
public class ServiceRegistry implements ServiceRegistryView {
  private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);

  private final PathTrie<Map<HttpRequestMethodType, ServerEndpoint>> endpoints;
  private String contextPath;
  private RequestMarshallerRegistry marshallerRegistry;

  public ServiceRegistry() {
    this.endpoints = new PathTrie<>();
    this.marshallerRegistry = new RequestMarshallerRegistry();
  }

  public void setContextPath(final String contextPath) {
    this.contextPath = contextPath;
  }

  public String getContextPath() {
    return contextPath;
  }

  public void setMarshallerRegistry(final RequestMarshallerRegistry marshallerRegistry) {
    this.marshallerRegistry = marshallerRegistry;
  }

  public RequestMarshallerRegistry getMarshallerRegistry() {
    return marshallerRegistry;
  }

  public ServerEndpoint findEndpoint(final String path, final HttpRequestMethodType requestMethodType, final Map<String, String> pathParams) {
    final Map<HttpRequestMethodType, ServerEndpoint> serviceEndpoints = endpoints.retrieve(path, pathParams);
    if (serviceEndpoints == null) {
      return null;
    }
    if (serviceEndpoints.containsKey(HttpRequestMethodType.ANY)) {
      return serviceEndpoints.get(HttpRequestMethodType.ANY);
    }
    return serviceEndpoints.get(requestMethodType);
  }

  public static class EndpointDescriptor {
    public final Method method;
    public final List<? extends ServiceFilter> filters;
    public final HttpRequestMethodType requestMethodType;

    public EndpointDescriptor(final Method method,
                              final List<? extends ServiceFilter> filters,
                              final HttpRequestMethodType requestMethodType) {
      this.method = method;
      this.filters = filters;
      this.requestMethodType = requestMethodType;
    }
  }

  public void registerEndpoints(final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> descriptors,
                                final String name, final Service service,
                                final List<AsyncFilter> asyncFilters,
                                final List<StreamFilter> streamFilters,
                                final boolean bindPrefix) {

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

      final String path = buildPath(name, methodBind);

      final Map<HttpRequestMethodType, EndpointDescriptor> endpointDescriptors = descriptors.get(methodBind);
      final Map<HttpRequestMethodType, ServerEndpoint> endpointsMap = new HashMap<>();

      if (endpointDescriptors.containsKey(HttpRequestMethodType.ANY) && endpointDescriptors.size() > 1) {
        throw new RuntimeException("Cannot add more request methods for the path after defining an ANY (all) endpoint path");
      }

      for (final Map.Entry<HttpRequestMethodType, EndpointDescriptor> endpointDescriptorEntry : endpointDescriptors.entrySet()) {
        final EndpointDescriptor endpointDesc = endpointDescriptorEntry.getValue();
        final Method method = endpointDesc.method;
        final List<String> methodParamNames = methodsParams.get(method);

        try {
          marshallerRegistry.registerTypes(TypeHelper.extractTypes(method));
        } catch (final IllegalArgumentException e) {
          final String serviceName = service.getClass().getName();
          throw new RuntimeException("Failed registering method '" + method.getName() +
            "' of service '" + serviceName + "': " + e.getMessage());
        }

        validateMethodParams(methodBind, endpointDesc, method, methodParamNames);

        final String[] params = methodParamNames.toArray(new String[methodParamNames.size()]);
        if (isAsyncMethod(method)) {
          endpointsMap.put(endpointDescriptorEntry.getKey(),
            new AsyncServerEndpoint(service, getFilters(endpointDesc.filters, asyncFilters, methodBind,
              AsyncFilter.class), method, endpointDesc.requestMethodType, params));
        } else if (isStreamingMethod(method)) {
          endpointsMap.put(endpointDescriptorEntry.getKey(),
            new StreamServerEndpoint(service, getFilters(endpointDesc.filters, streamFilters, methodBind,
              StreamFilter.class), method, endpointDesc.requestMethodType, params));
        } else {
          logger.warn("Will not register service endpoint {}::{}" +
            ". Method must return ComposableFuture or Observable!", name, method);

        }

      }

      endpoints.insert(path, endpointsMap, bindPrefix);
    }
  }

  private String buildPath(final String name, final String methodBind) {
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
    return path.toString();
  }

  private void validateMethodParams(final String methodBind, final EndpointDescriptor endpointDesc, final Method method, final List<String> methodParamNames) {
    final Class<?>[] parameterTypes = method.getParameterTypes();
    if (parameterTypes.length == 1 && parameterTypes[0] == Request.class) {
      return;
    }
    if (Arrays.asList(parameterTypes).contains(Request.class)) {
      throw new RuntimeException("Request object must be the only param in the method signature");
    }
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

        final Class paramType = parameterTypes[methodParamNames.indexOf(pathParameter)];

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

  @SuppressWarnings("unchecked")
  private static <T extends ServiceFilter> T[] getFilters(final List<? extends ServiceFilter> filters,
                                                          final List<T> serviceLevelFilters,
                                                          final String methodName,
                                                          final Class<T> filterType) {
    int size = 0;
    if (filters != null) {
      size += filters.size();
    }
    if (serviceLevelFilters != null) {
      size += serviceLevelFilters.size();
    }
    final Object result = Array.newInstance(filterType, size);
    int index = 0;
    if (filters != null) {
      for (final ServiceFilter filter : filters) {
        if (filterType.isAssignableFrom(filter.getClass())) {
          Array.set(result, index++, filter);
        } else {
          throw new RuntimeException("method " + methodName + " can only receive async filters");
        }
      }
    }
    if (serviceLevelFilters != null) {
      for (final T filter : serviceLevelFilters) {
        Array.set(result, index++, filter);
      }
    }
    return (T[]) result;
  }

  public void register(final String name,
                       final Service service,
                       final List<AsyncFilter> asyncFilters,
                       final List<StreamFilter> streamFilters,
                       final boolean bindPrefix) {

    final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> descriptors = getEndpointsDescriptor(service,
      asyncFilters, streamFilters);
    registerEndpoints(descriptors, name, service, null, null, bindPrefix);
  }

  private Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> getEndpointsDescriptor(final Service service,
                                                                                             final List<AsyncFilter> asyncFilters,
                                                                                             final List<StreamFilter> streamFilters) {

    final Method[] methods = service.getClass().getDeclaredMethods();
    final Map<String, Map<HttpRequestMethodType, EndpointDescriptor>> result = new HashMap<>();

    for (final Method m : methods) {
      if (isEndpoint(m)) {
        if (isAsyncMethod(m)) {
          result.put(m.getName(), singleEndpointDescToMap(new EndpointDescriptor(m, asyncFilters, HttpRequestMethodType.ANY)));
        } else if (isStreamingMethod(m)) {
          result.put(m.getName(), singleEndpointDescToMap(new EndpointDescriptor(m, streamFilters, HttpRequestMethodType.ANY)));
        }  else {
          logger.warn("Will not to register service endpoint {}::{}" +
            ". Method must return ComposableFuture or Observable!", service.getClass().getSimpleName(), m.getName());
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

  public SortedMap<String, Map<HttpRequestMethodType, ServerEndpointView>> getRegisteredEndpoints() {
    final SortedMap<String, Map<HttpRequestMethodType, ServerEndpointView>> copy = new TreeMap<>();
    for (final Map.Entry<String, Map<HttpRequestMethodType, ServerEndpoint>> entry : endpoints.getPathToValueMapping().entrySet()) {
      copy.put(entry.getKey(), Maps.transformValues(entry.getValue(), (Function<ServerEndpoint, ServerEndpointView>) input -> input));
    }
    return unmodifiableSortedMap(copy);
  }

  public void logRegisteredEndpoints() {
    for (final Map.Entry<String, Map<HttpRequestMethodType, ServerEndpointView>> pathEndpointsMap : getRegisteredEndpoints().entrySet()) {
      for (final Map.Entry<HttpRequestMethodType, ? extends ServerEndpointView> endpointEntry : pathEndpointsMap.getValue().entrySet()) {
        final ServerEndpointView endpointValue = endpointEntry.getValue();
        logger.info("Registered endpoint [{} ==> {}, via method: {}]", pathEndpointsMap.getKey(), endpointValue.getTargetAsString(), endpointValue.getRequestMethodType());
      }
    }
  }
}
