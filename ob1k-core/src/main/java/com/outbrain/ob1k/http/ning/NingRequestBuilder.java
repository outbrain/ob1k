package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.outbrain.ob1k.http.utils.ComposableFutureAdapter.fromListenableFuture;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.HttpResponseBodyPart;
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
import com.outbrain.ob1k.http.common.Header;
import com.outbrain.ob1k.http.common.Param;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import com.outbrain.ob1k.http.utils.ComposableFutureAdapter.Provider;
import com.outbrain.ob1k.http.utils.UrlUtils;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import org.apache.commons.codec.EncoderException;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * @author marenzon
 */
public class NingRequestBuilder implements RequestBuilder {

  public static final String USER_AGENT_HEADER = "User-Agent";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  private final AsyncHttpClient asyncHttpClient;
  private final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder;
  private final MetricFactory metricFactory;
  private MarshallingStrategy marshallingStrategy;
  private String requestUrl;
  private long responseMaxSize;

  public NingRequestBuilder(final AsyncHttpClient asyncHttpClient, final AsyncHttpClient.BoundRequestBuilder ningRequestBuilder,
                            final String requestUrl, final MetricFactory metricFactory, final long responseMaxSize,
                            final MarshallingStrategy marshallingStrategy) {

    this.asyncHttpClient = checkNotNull(asyncHttpClient, "asyncHttpClient may not be null");
    this.ningRequestBuilder = checkNotNull(ningRequestBuilder, "ningRequestBuilder may not be null");
    this.requestUrl = checkNotNull(requestUrl, "requestUrl may not be null");
    this.metricFactory = metricFactory;
    this.responseMaxSize = responseMaxSize;
    this.marshallingStrategy = marshallingStrategy;
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
  public RequestBuilder setPathParam(final String param, final String value) throws EncoderException {

    requestUrl = UrlUtils.replacePathParam(requestUrl, param, value);
    ningRequestBuilder.setUrl(requestUrl);
    return this;
  }

  @Override
  public RequestBuilder setPathParam(final Param param) throws EncoderException {

    return setPathParam(param.getName(), param.getValue());
  }

  @Override
  public RequestBuilder setPathParams(final List<Param> params) throws EncoderException {

    for (final Param param : params) {

      setPathParam(param);
    }

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
  public RequestBuilder addHeader(final Header header) {

    return addHeader(header.getName(), header.getValue());
  }

  @Override
  public RequestBuilder addHeaders(final List<Header> headers) {

    for (final Header header : headers) {

      addHeader(header);
    }

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
  public RequestBuilder setBody(final Object value) throws IOException {

    return setBody(marshallingStrategy.marshall(value));
  }

  @Override
  public RequestBuilder setBodyEncoding(final String charset) {

    ningRequestBuilder.setBodyEncoding(charset);
    return this;
  }

  @Override
  public RequestBuilder setContentLength(final int length) {

    ningRequestBuilder.setContentLength(length);
    return this;
  }

  @Override
  public RequestBuilder addQueryParam(final String name, final String value) {

    ningRequestBuilder.addQueryParam(name, value);
    return this;
  }

  @Override
  public RequestBuilder addQueryParams(final Map<String, String> params) {

    for (final Map.Entry<String, String> param : params.entrySet()) {

      addQueryParam(param.getKey(), param.getValue());
    }

    return this;
  }

  @Override
  public RequestBuilder addQueryParam(final Param param) {

    return addQueryParam(param.getName(), param.getValue());
  }

  @Override
  public RequestBuilder addQueryParams(final List<Param> params) {

    for (final Param param : params) {

      addQueryParam(param);
    }

    return this;
  }

  @Override
  public RequestBuilder setResponseMaxSize(final long responseMaxSize) {

    this.responseMaxSize = responseMaxSize;
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
  public RequestBuilder setMarshallingStrategy(final MarshallingStrategy marshallingStrategy) {

    this.marshallingStrategy = checkNotNull(marshallingStrategy, "unmarshallingStrategy may not be null");
    return this;
  }

  @Override
  public ComposableFuture<Response> asResponse() {

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
  public Observable<Response> asStream() {

    setRequestTimeout(-1);

    final PublishSubject<Response> result = PublishSubject.create();
    final NingHttpStreamHandler handler = new NingHttpStreamHandler(responseMaxSize, result);

    ningRequestBuilder.execute(handler);
    return result;
  }

  @Override
  public <T> ComposableFuture<TypedResponse<T>> asTypedResponse(final Class<T> type) {

    return asTypedResponse((Type) type);
  }

  @Override
  public <T> Observable<TypedResponse<T>> asTypedStream(final Class<T> type) {

    return asTypedStream((Type) type);
  }

  @Override
  public <T> ComposableFuture<TypedResponse<T>> asTypedResponse(final Type type) {

    final ComposableFuture<com.ning.http.client.Response> responseFuture = executeAndTransformRequest();

    return responseFuture.continueOnSuccess(new FutureSuccessHandler<com.ning.http.client.Response, TypedResponse<T>>() {
      @Override
      public ComposableFuture<TypedResponse<T>> handle(final com.ning.http.client.Response ningResponse) {
        try {
          final TypedResponse<T> response = new NingResponse<>(ningResponse, type, marshallingStrategy);
          return ComposableFutures.fromValue(response);
        } catch (final IOException e) {
          return ComposableFutures.fromError(e);
        }
      }
    });
  }

  @Override
  public <T> Observable<TypedResponse<T>> asTypedStream(final Type type) {

    setRequestTimeout(-1);

    final PublishSubject<TypedResponse<T>> result = PublishSubject.create();
    final NingHttpTypedStreamHandler<T> handler = new NingHttpTypedStreamHandler<>(responseMaxSize, result,
            marshallingStrategy, type);

    ningRequestBuilder.execute(handler);
    return result;
  }

  @Override
  public <T> ComposableFuture<T> asValue(final Class<T> type) {

    return asValue((Type) type);
  }

  @Override
  public <T> ComposableFuture<T> asValue(final Type type) {

    final ComposableFuture<TypedResponse<T>> responseFuture = asTypedResponse(type);

    return responseFuture.continueOnSuccess(new FutureSuccessHandler<TypedResponse<T>, T>() {
      @Override
      public ComposableFuture<T> handle(final TypedResponse<T> typedResponse) {
        try {
          return ComposableFutures.fromValue(typedResponse.getTypedBody());
        } catch (final IOException e) {
          return ComposableFutures.fromError(e);
        }
      }
    });
  }

  @Override
  public <T> Observable<T> asStreamValue(final Class<T> type) {

    return asStreamValue((Type) type);
  }

  @Override
  public <T> Observable<T> asStreamValue(final Type type) {

    final Observable<TypedResponse<T>> responseObservable = asTypedStream(type);

    return responseObservable.flatMap(new Func1<TypedResponse<T>, Observable<T>>() {
      @Override
      public Observable<T> call(final TypedResponse<T> typedResponse) {
        try {
          return Observable.from(typedResponse.getTypedBody());
        } catch (final IOException e) {
          return Observable.error(e);
        }
      }
    });
  }

  private com.ning.http.client.cookie.Cookie transformToNingCookie(final Cookie cookie) {

    return com.ning.http.client.cookie.Cookie.newValidCookie(cookie.getName(), cookie.getValue(), cookie.getDomain(),
            cookie.getValue(), cookie.getPath(), cookie.getExpires(),
            cookie.getMaxAge(), cookie.isSecure(), cookie.isHttpOnly());
  }

  private ComposableFuture<com.ning.http.client.Response> executeAndTransformRequest() {

    final Request ningRequest = ningRequestBuilder.build();

    return fromListenableFuture(new Provider<com.ning.http.client.Response>() {
        private boolean aborted = false;
        private long size;

        @Override
        public ListenableFuture<com.ning.http.client.Response> provide() {
          return asyncHttpClient.executeRequest(ningRequest, new AsyncCompletionHandler<com.ning.http.client.Response>() {
            @Override
            public com.ning.http.client.Response onCompleted(final com.ning.http.client.Response response) throws Exception {
              if (aborted) {
                throw new RuntimeException("Response size is bigger than the limit: " + responseMaxSize);
              }
              return response;
            }

            @Override
            public STATE onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
              if (responseMaxSize > 0) {
                size += content.length();
                if (size > responseMaxSize) {
                  aborted = true;
                  return STATE.ABORT;
                }
              }
              return super.onBodyPartReceived(content);
            }
          });
        }
      }, metricFactory, ningRequest.getUri().getHost()
    );
  }
}