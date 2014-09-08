package com.outbrain.ob1k.client;

import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.endpoints.AsyncClientEndpoint;
import com.outbrain.ob1k.client.endpoints.StreamClientEndpoint;
import com.outbrain.ob1k.client.endpoints.SyncClientEndpoint;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.common.marshalling.TypeHelper;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

import java.io.Closeable;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: aronen
 * Date: 6/18/13
 * Time: 2:18 PM
 */
public class ClientBuilder<T extends Service> {
  public static final int RETRIES = 3;
  public static final int CONNECTION_TIMEOUT = 200;
  public static final int REQUEST_TIMEOUT = 500;

  private final Class<T> type;
  private final List<String> targets;
  private final List<SyncFilter> syncFilters;
  private final List<AsyncFilter> asyncFilters;
  private final List<StreamFilter> streamFilters;
  private final Map<String, List<ServiceFilter>> methodBasedFilters;
  private final Map<String, String> explicitMethodBinding;

  private int retries = RETRIES;
  private int connectionTimeout = CONNECTION_TIMEOUT;
  private int requestTimeout = REQUEST_TIMEOUT;
  private ContentType clientType = ContentType.JSON; // default content type.
  private boolean compression = false;

  public ClientBuilder(final Class<T> type) {
    this.type = type;
    this.targets = new ArrayList<>();
    this.syncFilters = new ArrayList<>();
    this.asyncFilters = new ArrayList<>();
    this.streamFilters = new ArrayList<>();
    this.explicitMethodBinding = new HashMap<>();
    this.methodBasedFilters = new HashMap<>();
  }

  public ClientBuilder<T> addFilter(final ServiceFilter filter) {
    if (filter instanceof SyncFilter) {
      syncFilters.add((SyncFilter) filter);
    }

    if (filter instanceof AsyncFilter) {
      asyncFilters.add((AsyncFilter) filter);
    }

    if (filter instanceof StreamFilter) {
      streamFilters.add((StreamFilter) filter);
    }

    return this;
  }

  public ClientBuilder<T> addFilter(final ServiceFilter filter, final String methodName) {
    List<ServiceFilter> filters = methodBasedFilters.get(methodName);
    if (filters == null) {
      filters = new ArrayList<>();
      methodBasedFilters.put(methodName, filters);
    }
    filters.add(filter);

    return this;
  }

  public ClientBuilder<T> setProtocol(final ContentType clientType) {
    this.clientType = clientType;
    return this;
  }

  public ClientBuilder<T> setRetries(final int retries) {
    this.retries = retries;
    return this;
  }

  public ClientBuilder<T> setConnectionTimeout(final int timeout) {
    this.connectionTimeout = timeout;
    return this;
  }

  public ClientBuilder<T> setRequestTimeout(final int timeout) {
    this.requestTimeout = timeout;
    return this;
  }

  public ClientBuilder<T> addTarget(final String target) {
    this.targets.add(target);
    return this;
  }

  public ClientBuilder<T> bindEndpoint(final String methodNme, final String path) {
    explicitMethodBinding.put(methodNme, path);
    return this;
  }

  public ClientBuilder<T> setCompression(final boolean compression) {
    this.compression = compression;
    return this;
  }

  public T build() {
      return createHttpClient();
  }

  private T createHttpClient() {
    final ClassLoader loader = ClientBuilder.class.getClassLoader();
    final HttpClient client = new HttpClient(createRegistry(type), retries, connectionTimeout, requestTimeout, compression);

    final Map<Method, AbstractClientEndpoint> endpoints = extractEndpointsFromType(type, client,
        asyncFilters, syncFilters, streamFilters, methodBasedFilters, clientType, explicitMethodBinding);

    final HttpInvocationHandler handler = new HttpInvocationHandler(targets, client, endpoints);

    @SuppressWarnings("unchecked")
    final T res = (T) Proxy.newProxyInstance(loader, new Class[] {type, Closeable.class}, handler);
    return res;
  }

  private static RequestMarshallerRegistry createRegistry(final Class type) {
    final RequestMarshallerRegistry registry = new RequestMarshallerRegistry();

    final Method[] methods = type.getDeclaredMethods();
    for (final Method method: methods) {
      registry.registerTypes(TypeHelper.extractTypes(method));
    }

    return registry;
  }

  private static Map<Method, AbstractClientEndpoint> extractEndpointsFromType(final Class type, final HttpClient client,
        final List<AsyncFilter> asyncFilters, final List<SyncFilter> syncFilters,
        final List<StreamFilter> streamFilters, final Map<String, List<ServiceFilter>> methodBasedFilters,
        final ContentType contentType, final Map<String, String> explicitMethodBindings) {

    final Map<Method, AbstractClientEndpoint> endpoints = new HashMap<>();
    final Method[] methods = type.getDeclaredMethods();
    for (final Method method : methods) {
      final int modifiers = method.getModifiers();
      if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
        final String methodName = method.getName();
        final String methodPath = explicitMethodBindings.containsKey(methodName) ?
            explicitMethodBindings.get(methodName) :
            methodName;

        if (isAsync(method)) {
          final List<AsyncFilter> filters = mergeFilters(AsyncFilter.class, asyncFilters, methodBasedFilters.get(methodName));
          endpoints.put(method, new AsyncClientEndpoint(method, type, client, filters.toArray(new AsyncFilter[filters.size()]), contentType, methodPath));
        } else if (isStreaming(method)) {
          final List<StreamFilter> filters = mergeFilters(StreamFilter.class, streamFilters, methodBasedFilters.get(methodName));
          endpoints.put(method, new StreamClientEndpoint(method, type, client, filters.toArray(new StreamFilter[filters.size()]), contentType, methodPath));
        } else {
          final List<SyncFilter> filters = mergeFilters(SyncFilter.class, syncFilters, methodBasedFilters.get(methodName));
          endpoints.put(method, new SyncClientEndpoint(method, type, client, filters.toArray(new SyncFilter[filters.size()]), contentType, methodPath));
        }
      }
    }

    return endpoints;
  }

  private static <T extends ServiceFilter> List<T> mergeFilters(final Class<T> filterType, final List<T> baseFilters, final List<ServiceFilter> specificFilters) {
    final List<T> filters = new ArrayList<>();
    filters.addAll(baseFilters);
    if (specificFilters != null) {
      for (final ServiceFilter filter : specificFilters) {
        if (filter instanceof AsyncFilter) {
          filters.add(filterType.cast(filter));
        }
      }
    }

    return filters;
  }

  private static boolean isAsync(final Method method) {
    return method.getReturnType() == ComposableFuture.class;
  }

  private static boolean isStreaming(final Method method) {
    return method.getReturnType() == Observable.class;
  }



}
