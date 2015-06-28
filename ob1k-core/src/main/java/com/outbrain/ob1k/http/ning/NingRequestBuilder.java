package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureSuccessHandler;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.marshalling.JacksonUnmarshallingStrategy;
import com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy;
import com.outbrain.ob1k.http.utils.ComposableFutureAdapter;
import com.outbrain.ob1k.http.utils.ComposableFutureAdapter.Provider;
import com.outbrain.ob1k.http.utils.UrlUtils;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.apache.commons.codec.EncoderException;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author marenzon
 */
public class NingRequestBuilder implements RequestBuilder {

  public static final String USER_AGENT_HEADER = "User-Agent";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  private UnmarshallingStrategy unmarshallingStrategy;
  private String requestUrl;
  private final AsyncHttpClient asyncHttpClient;
  private final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder;
  private final MetricFactory metricFactory;

  public NingRequestBuilder(final AsyncHttpClient asyncHttpClient, final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder,
                            final String requestUrl, final MetricFactory metricFactory) {

    this.asyncHttpClient = checkNotNull(asyncHttpClient, "asyncHttpClient may not be null");
    this.ningRequestBuilder = checkNotNull(ningRequestBuilder, "ningRequestBuilder may not be null");
    this.requestUrl = checkNotNull(requestUrl, "requestUrl may not be null");
    this.metricFactory = metricFactory;
  }

  @Override
  public RequestBuilder setContentType(final ContentType contentType) {

    return setContentType(contentType.requestEncoding());
  }

  @Override
  public RequestBuilder setContentType(final String contentType) {

    ningRequestBuilder.setHeader(CONTENT_TYPE_HEADER, contentType);
    return this;
  }

  @Override
  public RequestBuilder setPathParams(final Map<String, String> params) throws EncoderException {

    for (final Map.Entry<String, String> pathParam : params.entrySet()) {

      setPathParam(pathParam.getKey(), pathParam.getValue());
    }

    return this;
  }

  @Override
  public RequestBuilder setPathParam(final String param, final String value) throws EncoderException {

    requestUrl = UrlUtils.replacePathParam(requestUrl, param, value);
    ningRequestBuilder.setUrl(requestUrl);
    return this;
  }

  @Override
  public RequestBuilder setUserAgent(final String userAgent) {

    return addHeader(USER_AGENT_HEADER, userAgent);
  }

  @Override
  public RequestBuilder addHeader(final String name, final String value) {

    ningRequestBuilder.addHeader(name, value);
    return this;
  }

  @Override
  public RequestBuilder setBody(final String body) {

    ningRequestBuilder.setBody(body);
    return this;
  }

  @Override
  public RequestBuilder setBody(final byte[] body) {

    ningRequestBuilder.setBody(body);
    return this;
  }

  @Override
  public RequestBuilder setBody(final InputStream stream) {

    ningRequestBuilder.setBody(stream);
    return this;
  }

  @Override
  public RequestBuilder addQueryParam(final String name, final String value) {

    ningRequestBuilder.addQueryParam(name, value);
    return this;
  }

  @Override
  public RequestBuilder addCookie(final Cookie cookie) {

    final com.ning.http.client.cookie.Cookie ningCookie = transformToNingCookie(cookie);
    ningRequestBuilder.addCookie(ningCookie);
    return this;
  }

  @Override
  public RequestBuilder setRequestTimeout(final int requestTimeout) {

    ningRequestBuilder.setRequestTimeout(requestTimeout);
    return this;
  }

  @Override
  public RequestBuilder setUnmarshallingStrategy(final UnmarshallingStrategy unmarshallingStrategy) {

    this.unmarshallingStrategy = unmarshallingStrategy;
    return this;
  }

  @Override
  public ComposableFuture<Response> execute() {

    final ComposableFuture<com.ning.http.client.Response> responseFuture = executeAndTransformRequest();

    return responseFuture.continueOnSuccess(new FutureSuccessHandler<com.ning.http.client.Response, Response>() {
      @Override
      public ComposableFuture<Response> handle(final com.ning.http.client.Response ningResponse) {
        try {
          final Response response = new NingResponse<>(ningResponse, null, null);
          return ComposableFutures.fromValue(response);
        } catch (final IOException e) {
          return ComposableFutures.fromError(e);
        }
      }
    });
  }

  @Override
  public Observable<Response> executeStream() {

    setRequestTimeout(-1);

    final PublishSubject<Response> result = PublishSubject.create();
    final NingHttpStreamHandler handler = new NingHttpStreamHandler(result);

    ningRequestBuilder.execute(handler);
    return result;
  }

  @Override
  public <T> ComposableFuture<TypedResponse<T>> executeTyped(final Class<T> type) {

    return executeTyped((Type) type);
  }

  @Override
  public <T> Observable<TypedResponse<T>> executeStream(final Class<T> type) {

    return executeStream((Type) type);
  }

  @Override
  public <T> ComposableFuture<TypedResponse<T>> executeTyped(final Type type) {

    if (unmarshallingStrategy == null) {

      unmarshallingStrategy = createDefaultUnmarshallingStrategy();
    }

    final ComposableFuture<com.ning.http.client.Response> responseFuture = executeAndTransformRequest();

    return responseFuture.continueOnSuccess(new FutureSuccessHandler<com.ning.http.client.Response, TypedResponse<T>>() {
      @Override
      public ComposableFuture<TypedResponse<T>> handle(final com.ning.http.client.Response ningResponse) {
        try {
          final TypedResponse<T> response = new NingResponse<>(ningResponse, type, unmarshallingStrategy);
          return ComposableFutures.fromValue(response);
        } catch (final IOException e) {
          return ComposableFutures.fromError(e);
        }
      }
    });
  }

  @Override
  public <T> Observable<TypedResponse<T>> executeStream(final Type type) {

    if (unmarshallingStrategy == null) {

      unmarshallingStrategy = createDefaultUnmarshallingStrategy();
    }

    setRequestTimeout(-1);

    final PublishSubject<TypedResponse<T>> result = PublishSubject.create();
    final NingHttpStreamHandlerTyped<T> handler = new NingHttpStreamHandlerTyped<>(result, unmarshallingStrategy, type);

    ningRequestBuilder.execute(handler);
    return result;
  }

  private com.ning.http.client.cookie.Cookie transformToNingCookie(final Cookie cookie) {

    final long expires = System.currentTimeMillis() + (cookie.getMaxAge() * 1000);

    return com.ning.http.client.cookie.Cookie.newValidCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
            cookie.getValue(), cookie.getPath(), expires, cookie.getMaxAge(), cookie.isSecure(),
            cookie.isHttpOnly()
    );
  }

  private ComposableFuture<com.ning.http.client.Response> executeAndTransformRequest() {

    final Request ningRequest = ningRequestBuilder.build();

    return ComposableFutureAdapter.fromListenableFuture(
            new Provider<com.ning.http.client.Response>() {
              @Override
              public ListenableFuture<com.ning.http.client.Response> provide() {
                return asyncHttpClient.executeRequest(ningRequest);
              }
            }, metricFactory, ningRequest.getUri().getHost()
    );
  }

  private UnmarshallingStrategy createDefaultUnmarshallingStrategy() {

    return new JacksonUnmarshallingStrategy();
  }
}