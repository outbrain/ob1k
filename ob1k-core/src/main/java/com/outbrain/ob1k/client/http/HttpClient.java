package com.outbrain.ob1k.client.http;

import com.google.common.base.Objects;
import com.ning.http.client.*;
import com.ning.http.client.providers.netty.NettyAsyncHttpProvider;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.client.ClientBuilder;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.handlers.SuccessHandler;
import rx.Observable;
import rx.subjects.PublishSubject;

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
public class HttpClient {
  private final AsyncHttpClient asyncHttpClient;
  private final RequestBuilder builder;

  public HttpClient() {
    this(ClientBuilder.RETRIES, ClientBuilder.CONNECTION_TIMEOUT, ClientBuilder.REQUEST_TIMEOUT);
  }

  public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout) {
    this(reties, connectionTimeout, requestTimeout, false);
  }

  public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout, final boolean compression) {
    this(new RequestMarshallerRegistry(), reties, connectionTimeout, requestTimeout, compression);
  }

  public HttpClient(final RequestMarshallerRegistry registry, final int reties, final int connectionTimeout,
                    final int requestTimeout, final boolean compression) {
    this(registry, reties, connectionTimeout, requestTimeout, compression, false);
  }

  public HttpClient(final int reties, final int connectionTimeout, final int requestTimeout,
                    final boolean compression, final boolean useRawUrl) {
    this(new RequestMarshallerRegistry(), reties, connectionTimeout, requestTimeout, compression, useRawUrl);
  }

  public HttpClient(final RequestMarshallerRegistry registry, final int reties, final int connectionTimeout,
                    final int requestTimeout, final boolean compression, final boolean useRawUrl) {
    this(registry, reties, connectionTimeout, requestTimeout, compression, useRawUrl, false);
  }

  public HttpClient(final RequestMarshallerRegistry registry, final int reties, final int connectionTimeout,
                    final int requestTimeout, final boolean compression, final boolean useRawUrl,
                    final boolean followRedirect) {

    final AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().
        setConnectionTimeoutInMs(connectionTimeout).
        setMaxRequestRetry(reties).
        setRequestTimeoutInMs(requestTimeout).
        setCompressionEnabled(compression).
        setUseRawUrl(useRawUrl).
        setFollowRedirects(followRedirect).
        build();

    final AsyncHttpProvider provider = HttpProviderHolder.INSTANCE;
    asyncHttpClient = new AsyncHttpClient(provider, config);
    this.builder = new RequestBuilder(registry);
  }

  private static class HttpProviderHolder {
    static AsyncHttpProvider INSTANCE = createProvider();
    static AsyncHttpProvider createProvider() {
      final NettyAsyncHttpProviderConfig nettyConfig = new NettyAsyncHttpProviderConfig();
      final AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setAsyncHttpClientProviderConfig(nettyConfig).build();
      return new NettyAsyncHttpProvider(config);
    }
  }

  public ComposableFuture<Response> httpGet(final String url) {
    return httpGet(url, (Param[])null);
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
        for(final Header header : headers) {
          requestBuilder.addHeader(header.key, header.value);
        }
      }
      final ListenableFuture<Response> future = requestBuilder.execute();
      return ComposableFutureAdaptor.fromListenableFuture(future);
    } catch (final IOException e) {
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

  public ComposableFuture httpGet(final String url, final Type respType, final String contentType, final List<String> methodParamNames,
                                         final Object[] params) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildGetRequestWithParams(asyncHttpClient, url, methodParamNames,
              params, contentType);
      final ListenableFuture<Response> future = requestBuilder.execute();
      return ComposableFutureAdaptor.fromListenableFuture(future).continueOnSuccess(new SuccessHandler<Response,Object>() {
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

  public ComposableFuture<Response> httpPost(final String url, final String body, final ContentType contentType, final Header... headers) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder =
              builder.buildPostRequest(asyncHttpClient, url, body, contentType.requestEncoding());

      if (headers != null && headers.length > 0) {
        for(final Header header : headers) {
          requestBuilder.addHeader(header.key, header.value);
        }
      }

      final ListenableFuture<Response> future = requestBuilder.execute();
      return ComposableFutureAdaptor.fromListenableFuture(future);
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
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = this.builder.buildPostRequestWithParams(asyncHttpClient, url, contentType.requestEncoding(), new Object[] {body});
      final ListenableFuture<Response> future = requestBuilder.execute();
      return ComposableFutureAdaptor.fromListenableFuture(future);
    } catch (final IOException e) {
      return ComposableFutures.fromError(e);
    }
  }

  public <T,R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final T body, final ContentType contentType) {
    return httpPost(url, respType, body, contentType, true, (Header[])null);
  }

  public <T,R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final T body, final ContentType contentType, final boolean failOnError, final Header... headers) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = this.builder.buildPostRequestWithParams(asyncHttpClient, url, contentType.requestEncoding(), new Object[] {body});
      if (headers != null && headers.length > 0) {
        for(final Header header : headers) {
          requestBuilder.addHeader(header.key, header.value);
        }
      }

      final ListenableFuture<Response> future = requestBuilder.execute();
      final ComposableFuture<Response> futureResp = ComposableFutureAdaptor.fromListenableFuture(future);

      return futureResp.continueOnSuccess(new SuccessHandler<Response, R>() {
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
    return httpPost(url, respType, body, contentType, (Header[])null);
  }

  public <R> ComposableFuture<R> httpPost(final String url, final Class<R> respType, final String body, final ContentType contentType, final Header... headers) {
    final ComposableFuture<Response> result = httpPost(url, body, contentType, (Header[])headers);
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
    return httpPost(url, respType, body, ContentType.JSON, (Header[])headers);
  }

  /**
   * untyped response on purpose since it's being used by a dynamic proxy which is generic anyway,
   * @param url the endpoint
   * @param respType the java type of the returned value to be used by the relevant un-marshaller.
   * @param params the set of value that will be marshaled inside the request body
   * @param contentType the marshaller/un-marshaller type.
   * @return the (future)response of the http request, un-marshaled into a java object
   */
  public ComposableFuture httpPost(final String url, final Type respType, final Object[] params, final String contentType) {
    final ComposableFuture<Response> result;
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder =
          builder.buildPostRequestWithParams(asyncHttpClient, url, contentType, params);
      final ListenableFuture<Response> future = requestBuilder.execute();
      result = ComposableFutureAdaptor.fromListenableFuture(future);
    } catch (final IOException e) {
      return ComposableFutures.fromError(e);
    }

    return result.continueOnSuccess(new SuccessHandler<Response, Object>() {
      @Override
      public Object handle(final Response response) throws ExecutionException {
        try {
          return builder.unmarshallResponse(response, respType);
        } catch (final IOException e) {
          throw new ExecutionException(e);
        }
      }
    });
  }

  public ComposableFuture httpPut(final String url, final Type respType, final Object[] params, final String contentType) {
    final ComposableFuture<Response> result;
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder =
              builder.buildPutRequestWithParams(asyncHttpClient, url, contentType, params);
      final ListenableFuture<Response> future = requestBuilder.execute();
      result = ComposableFutureAdaptor.fromListenableFuture(future);
    } catch (final IOException e) {
      return ComposableFutures.fromError(e);
    }

    return result.continueOnSuccess(new SuccessHandler<Response, Object>() {
      @Override
      public Object handle(final Response response) throws ExecutionException {
        try {
          return builder.unmarshallResponse(response, respType);
        } catch (final IOException e) {
          throw new ExecutionException(e);
        }
      }
    });
  }

  public ComposableFuture httpDelete(final String url, final Type respType, final String contentType, final List<String> methodParamNames,
                                            final Object[] params) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildDeleteRequestWithParams(asyncHttpClient, url, methodParamNames,
              params, contentType);
      final ListenableFuture<Response> future = requestBuilder.execute();
      return ComposableFutureAdaptor.fromListenableFuture(future).continueOnSuccess(new SuccessHandler<Response,Object>() {
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

  public Observable httpGetStreaming(final String url, final Type respType, final String contentType, final List<String> methodParamNames,
                                         final Object[] params) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildGetRequestWithParams(asyncHttpClient, url, methodParamNames,
              params, contentType);

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

  public Observable httpDeleteStreaming(final String url, final Type respType, final String contentType, final List<String> methodParamNames,
                                     final Object[] params) {
    try {
      final AsyncHttpClient.BoundRequestBuilder requestBuilder = builder.buildDeleteRequestWithParams(asyncHttpClient, url, methodParamNames,
              params, contentType);

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
