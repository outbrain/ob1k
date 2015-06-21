package com.outbrain.ob1k.client;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.endpoints.AbstractClientEndpoint;
import com.outbrain.ob1k.client.endpoints.AsyncClientEndpoint;
import com.outbrain.ob1k.client.endpoints.StreamClientEndpoint;
import com.outbrain.ob1k.client.endpoints.SyncClientEndpoint;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.client.targets.EmptyTargetProvider;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.common.marshalling.TypeHelper;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.MethodParamNamesExtractor;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import rx.Observable;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
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
    private TargetProvider targetProvider = new EmptyTargetProvider();
    private final List<SyncFilter> syncFilters;
    private final List<AsyncFilter> asyncFilters;
    private final List<StreamFilter> streamFilters;
    private final Map<String, EndpointDescriptor> endpointDescriptors;

    private int retries = RETRIES;
    private int connectionTimeout = CONNECTION_TIMEOUT;
    private boolean followRedirect = false;
    private int maxConnectionsPerHost = HttpClient.MAX_CONNECTIONS_PER_HOST;
    private int totalMaxConnections = HttpClient.TOTAL_MAX_CONNECTIONS;
    private int requestTimeout = REQUEST_TIMEOUT;
    private ContentType clientType = ContentType.JSON; // default content type.
    private boolean compression = false;
    private MetricFactory metricFactory;

    public ClientBuilder(final Class<T> type) {
        this.type = type;
        this.syncFilters = new ArrayList<>();
        this.asyncFilters = new ArrayList<>();
        this.streamFilters = new ArrayList<>();
        this.endpointDescriptors = new HashMap<>();
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

    public ClientBuilder<T> setProtocol(final ContentType clientType) {
        this.clientType = clientType;
        return this;
    }

    public ClientBuilder<T> followRedirect(final boolean followRedirect) {
        this.followRedirect = followRedirect;
        return this;
    }

    public ClientBuilder<T> setMetricFactory(final MetricFactory metricFactory) {
        this.metricFactory = metricFactory;
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

    public ClientBuilder<T> setTargetProvider(final TargetProvider targetProvider) {
        this.targetProvider = targetProvider == null ? new EmptyTargetProvider() : targetProvider;
        return this;
    }

    public ClientBuilder<T> bindEndpoint(final String methodName, final String path, final ServiceFilter... filters) {
        bindEndpoint(methodName, HttpRequestMethodType.ANY, path, filters);
        return this;
    }

    public ClientBuilder<T> bindEndpoint(final String methodName, final HttpRequestMethodType requestMethodType) {
        bindEndpoint(methodName, requestMethodType, methodName);
        return this;
    }

    public ClientBuilder<T> bindEndpoint(final String methodName, final HttpRequestMethodType requestMethodType, final ServiceFilter... filters) {
        bindEndpoint(methodName, requestMethodType, methodName, filters);
        return this;
    }

    public ClientBuilder<T> bindEndpoint(final String methodName, final ServiceFilter... filters) {
        bindEndpoint(methodName, HttpRequestMethodType.ANY, methodName, filters);
        return this;
    }

    public ClientBuilder<T> bindEndpoint(final String methodName, final HttpRequestMethodType requestMethodType, final String path, final ServiceFilter... filters) {
        final List<? extends ServiceFilter> serviceFilters;
        if (filters == null) {
            serviceFilters = new ArrayList<>();
        } else {
            serviceFilters = Arrays.asList(filters);
        }
        endpointDescriptors.put(methodName, new EndpointDescriptor(methodName, path, serviceFilters, requestMethodType));
        return this;
    }

    public ClientBuilder<T> setCompression(final boolean compression) {
        this.compression = compression;
        return this;
    }

    public ClientBuilder<T> setMaxConnectionsPerHost(final int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        if (maxConnectionsPerHost > totalMaxConnections) {
            this.totalMaxConnections = maxConnectionsPerHost * 2;
        }

        return this;
    }

    public ClientBuilder<T> setTotalMaxConnections(final int maxConnections) {
        this.totalMaxConnections = maxConnections;
        return this;
    }

    public T build() {
        return createHttpClient();
    }

    private T createHttpClient() {
        final ClassLoader loader = ClientBuilder.class.getClassLoader();
        final HttpClient client = new HttpClient(createRegistry(type), retries, connectionTimeout, requestTimeout,
            compression, false, followRedirect, maxConnectionsPerHost, totalMaxConnections, metricFactory);

        final Map<Method, AbstractClientEndpoint> endpoints = extractEndpointsFromType(type, client,
            asyncFilters, syncFilters, streamFilters, clientType, endpointDescriptors);

        final HttpInvocationHandler handler = new HttpInvocationHandler(targetProvider, client, endpoints);

        @SuppressWarnings("unchecked")
        final T res = (T) Proxy.newProxyInstance(loader, new Class[]{type, Closeable.class}, handler);
        return res;
    }

    private static RequestMarshallerRegistry createRegistry(final Class type) {
        final RequestMarshallerRegistry registry = new RequestMarshallerRegistry();

        final Method[] methods = type.getDeclaredMethods();
        for (final Method method : methods) {
            registry.registerTypes(TypeHelper.extractTypes(method));
        }

        return registry;
    }

    private static Map<Method, AbstractClientEndpoint> extractEndpointsFromType(final Class type, final HttpClient client,
                                                                                final List<AsyncFilter> asyncFilters,
                                                                                final List<SyncFilter> syncFilters,
                                                                                final List<StreamFilter> streamFilters,
                                                                                final ContentType contentType,
                                                                                final Map<String, EndpointDescriptor> endpointDescriptors) {

        final Map<Method, AbstractClientEndpoint> endpoints = new HashMap<>();
        final Method[] methods = type.getDeclaredMethods();
        final Map<Method, List<String>> methodParams;
        try {
            methodParams = MethodParamNamesExtractor.extract(type, Arrays.asList(methods));
        } catch (final Exception e) {
            throw new RuntimeException("Service " + type.toString() + " can't be analyzed", e);
        }
        for (final Method method : methods) {
            final int modifiers = method.getModifiers();
            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                final String methodName = method.getName();
                final EndpointDescriptor methodDescriptor = endpointDescriptors.containsKey(methodName) ?
                    endpointDescriptors.get(methodName) :
                    new EndpointDescriptor(methodName, methodName, null, HttpRequestMethodType.ANY);
                if (isAsync(method)) {
                    final List<AsyncFilter> filters = mergeFilters(AsyncFilter.class, asyncFilters, methodDescriptor.filters);
                    endpoints.put(method, new AsyncClientEndpoint(method, methodParams.get(method), type, client, filters.toArray(new AsyncFilter[filters.size()]), contentType,
                        methodDescriptor.path, methodDescriptor.requestMethodType));
                } else if (isStreaming(method)) {
                    final List<StreamFilter> filters = mergeFilters(StreamFilter.class, streamFilters, methodDescriptor.filters);
                    endpoints.put(method, new StreamClientEndpoint(method, methodParams.get(method), type, client, filters.toArray(new StreamFilter[filters.size()]), contentType,
                        methodDescriptor.path, methodDescriptor.requestMethodType));
                } else {
                    final List<SyncFilter> filters = mergeFilters(SyncFilter.class, syncFilters, methodDescriptor.filters);
                    endpoints.put(method, new SyncClientEndpoint(method, methodParams.get(method), type, client, filters.toArray(new SyncFilter[filters.size()]), contentType,
                        methodDescriptor.path, methodDescriptor.requestMethodType));
                }
            }
        }

        return endpoints;
    }

    private static <T extends ServiceFilter> List<T> mergeFilters(final Class<T> filterType, final List<T> baseFilters, final List<? extends ServiceFilter> specificFilters) {
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

    /**
     * Describes how endpoint of service looks for the client builder
     *
     * @author marenzon
     */
    private static class EndpointDescriptor {
        public final String method;
        public final String path;
        public final List<? extends ServiceFilter> filters;
        public final HttpRequestMethodType requestMethodType;

        public EndpointDescriptor(final String method, final String path, final List<? extends ServiceFilter> filters, final HttpRequestMethodType requestMethodType) {
            this.method = method;
            this.path = path;
            this.filters = filters;
            this.requestMethodType = requestMethodType;
        }
    }
}
