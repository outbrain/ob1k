package com.outbrain.ob1k.client.http;

import com.google.common.base.Objects;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * User: aronen
 * Date: 6/16/13
 * Time: 3:41 PM
 */
public class HttpClient implements Closeable {
    public static final int MAX_CONNECTIONS_PER_HOST = 100;
    public static final int TOTAL_MAX_CONNECTIONS = MAX_CONNECTIONS_PER_HOST * 2;
    private final AsyncHttpClient asyncHttpClient;
    private final RequestBuilder builder;
    private final MetricFactory metricFactory;

    public HttpClient() {
        this(ClientBuilder.RETRIES, ClientBuilder.CONNECTION_TIMEOUT, ClientBuilder.REQUEST_TIMEOUT);
    }

    public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout) {
        this(reties, connectionTimeout, requestTimeout, false);
    }

    public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout, final MetricFactory metricFactory) {
        this(new RequestMarshallerRegistry(), reties, connectionTimeout, requestTimeout, false, false,
                false, MAX_CONNECTIONS_PER_HOST, TOTAL_MAX_CONNECTIONS, metricFactory);
    }

    public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout, final boolean compression) {
        this(new RequestMarshallerRegistry(), reties, connectionTimeout, requestTimeout, compression);
    }

    public HttpClient(final RequestMarshallerRegistry registry, final int reties, final int connectionTimeout,
                      final int requestTimeout, final boolean compression) {
        this(registry, reties, connectionTimeout, requestTimeout, compression, false, false,
                MAX_CONNECTIONS_PER_HOST, TOTAL_MAX_CONNECTIONS, null);
    }

    public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout,
                      final boolean compression, final boolean useRawUrl) {
        this(new RequestMarshallerRegistry(), reties, connectionTimeout, requestTimeout, compression, useRawUrl,
                false, MAX_CONNECTIONS_PER_HOST, TOTAL_MAX_CONNECTIONS, null);
    }

    public HttpClient(final RequestMarshallerRegistry registry, final int reties, final int connectionTimeout,
                      final int requestTimeout, final boolean compression, final boolean useRawUrl,
                      final boolean followRedirect, final int maxConnectionsPerHost, final int totalMaxConnections,
                      final MetricFactory metricFactory) {

        final AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().
            setConnectTimeout(connectionTimeout).
            setMaxRequestRetry(reties).
            setRequestTimeout(requestTimeout).
            setCompressionEnforced(compression).
            setDisableUrlEncodingForBoundedRequests(useRawUrl).
            setAsyncHttpClientProviderConfig(NettyConfigHolder.INSTANCE).
            setFollowRedirect(followRedirect).
            setMaxConnectionsPerHost(maxConnectionsPerHost).
            setMaxConnections(totalMaxConnections).
            build();

        this.asyncHttpClient = new AsyncHttpClient(config);
        this.builder = new RequestBuilder(registry);
        this.metricFactory = metricFactory;
    }

    private static class NettyConfigHolder {
        static final NettyAsyncHttpProviderConfig INSTANCE = createConfig();
        private static NettyAsyncHttpProviderConfig createConfig() {
            final NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
            final NioClientSocketChannelFactory channelFactory = new NioClientSocketChannelFactory();

            nettyConfig.setSocketChannelFactory(channelFactory);
            return nettyConfig;
        }
    }

    @Override
    public void close() {
        asyncHttpClient.close();
    }

    public ComposableFuture<Response> httpGet(final String url) {
        return httpGet(url, (Param[]) null);
    }

    public static class Param {
        public final String key;
        public final String value;

        public Param(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return Objects.toStringHelper(getClass()).add("key", key).add("value", value).toString();
        }
    }

    public static class Header {
        public final String key;
        public final String value;

        public Header(final String key, final String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static Param param(final String key, final String value) {
        return new Param(key, value);
    }

    public ComposableFuture<Response> httpGet(final String url, final Header[] headers, final Param[] params) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildGetRequest(asyncHttpClient, url, params);
            if (headers != null && headers.length > 0) {
                for (final Header header : headers) {
                    requestBuilder.addHeader(header.key, header.value);
                }
            }
            final Request request = requestBuilder.build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory);
        } catch (final Exception e) {
            return ComposableFutures.fromError(e);
        }
    }

    public <R> ComposableFuture<R> httpGet(final String url, final Class<R> respType, final Header[] headers, final Param[] params) {
        return httpGet(url, headers, params).continueOnSuccess(new SuccessHandler<Response, R>() {
            @Override
            public R handle(final Response response) throws ExecutionException {
                try {
                    return builder.unmarshallResponse(response, respType);
                } catch (final IOException e) {
                    throw new ExecutionException(e.getMessage(), e);
                }
            }
        });
    }

    public ComposableFuture httpGet(final String url, final Type respType, final String contentType,
                                    final List<String> methodParamNames, final Object[] params) {
        try {
            final Request request =
                builder.buildGetRequestWithParams(asyncHttpClient, url, methodParamNames, params, contentType).build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory).continueOnSuccess(new SuccessHandler<Response, Object>() {
                @Override
                public Object handle(final Response response) throws ExecutionException {
                    try {
                        return builder.unmarshallResponse(response, respType);
                    } catch (final IOException e) {
                        throw new ExecutionException(e.getMessage(), e);
                    }
                }
            });
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public <T> ComposableFuture<T> httpGet(final String url, final Class<T> respType) {
        return httpGet(url, respType, null, null);
    }

    public ComposableFuture<Response> httpGet(final String url, final Header... headers) {
        return httpGet(url, headers, null);
    }

    public ComposableFuture<Response> httpGet(final String url, final Param... params) {
        return httpGet(url, (Header[]) null, params);
    }

    public <T> ComposableFuture<T> httpGet(final String url, final Class<T> respType, final Header... headers) {
        return httpGet(url, respType, headers, null);
    }

    public <T> ComposableFuture<T> httpGet(final String url, final Class<T> respType, final Param... params) {
        return httpGet(url, respType, null, params);
    }

    public ComposableFuture<Response> httpGet(final String url, final Map<String, String> params) {
        return httpGet(url, toArray(params));
    }

    public <T> ComposableFuture<T> httpGet(final String url, final Class<T> respType, final Map<String, String> params) {
        return httpGet(url, respType, toArray(params));
    }

    public ComposableFuture<Response> httpHead(final String url, final Header... headers) {
        final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildHeadRequest(asyncHttpClient, url);
        final Request request = requestBuilder.build();

        if (headers != null && headers.length > 0) {
            for (final Header header : headers) {
                requestBuilder.addHeader(header.key, header.value);
            }
        }

        return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
            @Override
            public ListenableFuture<Response> provide() {
              return asyncHttpClient.executeRequest(request);
            }
        }, request, metricFactory);
    }

    public ComposableFuture<Response> httpPost(final String url, final String body, final ContentType contentType, final Header... headers) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildPostRequest(asyncHttpClient, url, body, contentType.requestEncoding());

            if (headers != null && headers.length > 0) {
                for (final Header header : headers) {
                    requestBuilder.addHeader(header.key, header.value);
                }
            }
            final Request request = requestBuilder.build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory);
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public ComposableFuture<Response> httpPost(final String url, final String body, final Header... headers) {
        return httpPost(url, body, ContentType.TEXT_PLAIN, (Header[]) headers);
    }

    public ComposableFuture<Response> httpPost(final String url, final String body, final ContentType contentType) {
        return httpPost(url, body, contentType, (Header[]) null);
    }

    public <T> ComposableFuture<Response> httpPost(final String url, final T body, final ContentType contentType) {
        try {
            final Request request =
                builder.buildPostRequestWithParams(asyncHttpClient, url, contentType.requestEncoding(), new Object[]{body}).build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory);
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public <T, R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final T body, final ContentType contentType) {
        return httpPost(url, respType, body, contentType, true, (Header[]) null);
    }

    public <T, R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final T body,
                                               final ContentType contentType, final boolean failOnError,
                                               final Header... headers) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                this.builder.buildPostRequestWithParams(asyncHttpClient, url, contentType.requestEncoding(), new Object[]{body});

            if (headers != null && headers.length > 0) {
                for (final Header header : headers) {
                    requestBuilder.addHeader(header.key, header.value);
                }
            }
            final Request request = requestBuilder.build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory).continueOnSuccess(new SuccessHandler<Response, R>() {
                @Override
                public R handle(final Response response) throws ExecutionException {
                    try {
                        return builder.unmarshallResponse(response, respType, failOnError);
                    } catch (final IOException e) {
                        throw new ExecutionException(e);
                    }
                }
            });
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public <R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final String body, final ContentType contentType) {
        return httpPost(url, respType, body, contentType, (Header[]) null);
    }

    public <R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final String body,
                                            final ContentType contentType, final Header... headers) {

        final ComposableFuture<Response> result = httpPost(url, body, contentType, (Header[]) headers);
        return result.continueOnSuccess(new SuccessHandler<Response, R>() {
            @Override
            public R handle(final Response response) throws ExecutionException {
                try {
                    return builder.unmarshallResponse(response, respType);
                } catch (final IOException e) {
                    throw new ExecutionException(e);
                }
            }
        });
    }

    public <R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final String body, final Header... headers) {
        return httpPost(url, respType, body, ContentType.JSON, (Header[]) headers);
    }

    /**
     * untyped response on purpose since it's being used by a dynamic proxy which is generic anyway,
     *
     * @param url         the endpoint
     * @param respType    the java type of the returned value to be used by the relevant un-marshaller.
     * @param params      the set of value that will be marshaled inside the request body
     * @param contentType the marshaller/un-marshaller type.
     * @return the (future)response of the http request, un-marshaled into a java object
     */
    public ComposableFuture httpPost(final String url, final Type respType, final Object[] params, final String contentType) {
        try {
            final Request request =
                builder.buildPostRequestWithParams(asyncHttpClient, url, contentType, params).build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory).continueOnSuccess(new SuccessHandler<Response, Object>() {
                @Override
                public Object handle(final Response response) throws ExecutionException {
                    try {
                        return builder.unmarshallResponse(response, respType);
                    } catch (final IOException e) {
                        throw new ExecutionException(e);
                    }
                }
            });
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public ComposableFuture httpPut(final String url, final Type respType, final Object[] params, final String contentType) {
        try {
            final Request request =
                builder.buildPutRequestWithParams(asyncHttpClient, url, contentType, params).build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory).continueOnSuccess(new SuccessHandler<Response, Object>() {
                @Override
                public Object handle(final Response response) throws ExecutionException {
                    try {
                        return builder.unmarshallResponse(response, respType);
                    } catch (final IOException e) {
                        throw new ExecutionException(e);
                    }
                }
            });
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public ComposableFuture<Response> httpPut(final String url, final String body, final ContentType contentType, final Header... headers) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildPutRequest(asyncHttpClient, url, body, contentType.requestEncoding());

            if (headers != null && headers.length > 0) {
                for (final Header header : headers) {
                    requestBuilder.addHeader(header.key, header.value);
                }
            }

            final Request request = requestBuilder.build();
            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory);
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public ComposableFuture httpDelete(final String url, final Type respType, final String contentType,
                                       final List<String> methodParamNames, final Object[] params) {
        try {
            final Request request =
                builder.buildDeleteRequestWithParams(asyncHttpClient, url, methodParamNames, params, contentType).build();

            return ComposableFutureAdaptor.fromListenableFuture(new ComposableFutureAdaptor.ListenableFutureProvider<Response>() {
                @Override
                public ListenableFuture<Response> provide() {
                    return asyncHttpClient.executeRequest(request);
                }
            }, request, metricFactory).continueOnSuccess(new SuccessHandler<Response, Object>() {
                @Override
                public Object handle(final Response response) throws ExecutionException {
                    try {
                        return builder.unmarshallResponse(response, respType);
                    } catch (final IOException e) {
                        throw new ExecutionException(e.getMessage(), e);
                    }
                }
            });
        } catch (final IOException e) {
            return ComposableFutures.fromError(e);
        }
    }

    public Observable httpPostStreaming(final String url, final Type respType, final Object[] params, final String contentType) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildPostRequestWithParams(asyncHttpClient, url, contentType, params);
            final PublishSubject<Object> result = PublishSubject.create();
            final HttpStreamHandler<Object> handler = new HttpStreamHandler<>(result, builder, respType);
            requestBuilder.execute(handler);

            return result;
        } catch (final IOException e) {
            return Observable.error(e);
        }
    }

    public Observable httpGetStreaming(final String url, final Type respType, final String contentType,
                                       final List<String> methodParamNames, final Object[] params) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildGetRequestWithParams(asyncHttpClient, url, methodParamNames, params, contentType);

            final PublishSubject<Object> result = PublishSubject.create();
            final HttpStreamHandler<Object> handler = new HttpStreamHandler<>(result, builder, respType);
            requestBuilder.execute(handler);
            return result;
        } catch (final IOException e) {
            return Observable.error(e);
        }
    }

    public Observable httpPutStreaming(final String url, final Type respType, final Object[] params, final String contentType) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildPutRequestWithParams(asyncHttpClient, url, contentType, params);

            final PublishSubject<Object> result = PublishSubject.create();
            final HttpStreamHandler<Object> handler = new HttpStreamHandler<>(result, builder, respType);
            requestBuilder.execute(handler);

            return result;
        } catch (final IOException e) {
            return Observable.error(e);
        }
    }

    public Observable httpDeleteStreaming(final String url, final Type respType, final String contentType,
                                          final List<String> methodParamNames, final Object[] params) {
        try {
            final AsyncHttpClient.BoundRequestBuilder requestBuilder =
                builder.buildDeleteRequestWithParams(asyncHttpClient, url, methodParamNames, params, contentType);

            final PublishSubject<Object> result = PublishSubject.create();
            final HttpStreamHandler<Object> handler = new HttpStreamHandler<>(result, builder, respType);
            requestBuilder.execute(handler);
            return result;
        } catch (final IOException e) {
            return Observable.error(e);
        }
    }

    private Param[] toArray(final Map<String, String> paramsMap) {
        final ArrayList<Param> paramsList = new ArrayList<>(paramsMap.size());
        for (final Map.Entry<String, String> entry : paramsMap.entrySet()) {
            paramsList.add(param(entry.getKey(), entry.getValue()));
        }
        return paramsList.toArray(new Param[paramsList.size()]);
    }

}
