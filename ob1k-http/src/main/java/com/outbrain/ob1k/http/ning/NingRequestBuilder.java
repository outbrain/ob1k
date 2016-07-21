package com.outbrain.ob1k.http.ning;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.common.Header;
import com.outbrain.ob1k.http.common.Param;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import com.outbrain.ob1k.http.utils.ComposableFutureAdapter.Provider;
import com.outbrain.ob1k.http.utils.UrlUtils;
import org.apache.commons.codec.EncoderException;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.BoundRequestBuilder;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Realm;
import org.asynchttpclient.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromError;
import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.http.utils.ComposableFutureAdapter.fromListenableFuture;

/**
 * @author marenzon
 */
public class NingRequestBuilder implements RequestBuilder {

  private static final Logger log = LoggerFactory.getLogger(RequestBuilder.class);

  private final AsyncHttpClient asyncHttpClient;
  private final BoundRequestBuilder ningRequestBuilder;

  private MarshallingStrategy marshallingStrategy;
  private String requestUrl;
  private long responseMaxSize;
  private Charset charset = DEFAULT_CHARSET;
  private String bodyString;
  private byte[] bodyByteArray;
  private Object bodyObject;

  public NingRequestBuilder(final AsyncHttpClient asyncHttpClient, final BoundRequestBuilder ningRequestBuilder,
                            final String requestUrl, final long responseMaxSize, final MarshallingStrategy marshallingStrategy) {

    this.asyncHttpClient = checkNotNull(asyncHttpClient, "asyncHttpClient may not be null");
    this.ningRequestBuilder = checkNotNull(ningRequestBuilder, "ningRequestBuilder may not be null");
    this.requestUrl = checkNotNull(requestUrl, "requestUrl may not be null");
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

    headers.forEach(this::addHeader);
    return this;
  }

  @Override
  public RequestBuilder setBody(final String body) {

    this.bodyString = body;
    return this;
  }

  @Override
  public RequestBuilder setBody(final byte[] body) {

    this.bodyByteArray = body;
    return this;
  }

  @Override
  public RequestBuilder setBody(final Object body) {

    this.bodyObject = body;
    return this;
  }

  @Override
  public RequestBuilder setBodyEncoding(final Charset charset) {

    this.charset = charset;
    return this;
  }

  @Override
  public RequestBuilder withBasicAuth(final String username, final String password) {

    final Realm realm = new Realm.Builder(username, password).
      setUsePreemptiveAuth(true).
      setScheme(Realm.AuthScheme.BASIC).
      build();

    ningRequestBuilder.setRealm(realm);
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

    params.forEach(this::addQueryParam);
    return this;
  }

  @Override
  public RequestBuilder setResponseMaxSize(final long responseMaxSize) {

    this.responseMaxSize = responseMaxSize;
    return this;
  }

  @Override
  public RequestBuilder addCookie(final Cookie cookie) {

    final org.asynchttpclient.cookie.Cookie ningCookie = transformToNingCookie(cookie);
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

    try {
      prepareRequestBody();
    } catch (final IOException e) {
      return fromError(e);
    }

    final ComposableFuture<org.asynchttpclient.Response> responseFuture = executeAndTransformRequest();

    return responseFuture.flatMap(ningResponse -> {
      final Response response = new NingResponse<>(ningResponse, null, null);
      return fromValue(response);
    });
  }

  @Override
  public Observable<Response> asStream() {

    try {
      prepareRequestBody();
    } catch (final IOException e) {
      return Observable.error(e);
    }

    // request timeout in streams is problematic, because of unknown dispatching intervals
    // not sure if should be disabled, but also cannot be low value
    setRequestTimeout(-1);

    final PublishSubject<Response> result = PublishSubject.create();
    final NingHttpResponseStreamHandler handler = new NingHttpResponseStreamHandler(responseMaxSize, result);

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

    final ComposableFuture<org.asynchttpclient.Response> responseFuture = executeAndTransformRequest();

    return responseFuture.flatMap(ningResponse -> {
      final TypedResponse<T> response = new NingResponse<>(ningResponse, type, marshallingStrategy);
      return fromValue(response);
    });
  }

  @Override
  public <T> Observable<TypedResponse<T>> asTypedStream(final Type type) {

    try {
      prepareRequestBody();
    } catch (final IOException e) {
      return Observable.error(e);
    }

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

    return responseFuture.flatMap(typedResponse -> {
      try {
        return fromValue(typedResponse.getTypedBody());
      } catch (final IOException e) {
        return fromError(e);
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

    return responseObservable.flatMap(typedResponse -> {
      try {
        return Observable.just(typedResponse.getTypedBody());
      } catch (final IOException e) {
        return Observable.error(e);
      }
    });
  }

  private ComposableFuture<org.asynchttpclient.Response> executeAndTransformRequest() {

    try {
      prepareRequestBody();
    } catch (final IOException e) {
      return fromError(e);
    }

    final Request ningRequest = ningRequestBuilder.build();

    if (log.isTraceEnabled()) {
      final String body = ningRequest.getByteData() == null
        ? ningRequest.getStringData() :
        new String(ningRequest.getByteData(), Charset.forName(charset));

      log.trace("Sending HTTP call to {}: headers=[{}], body=[{}]", ningRequest.getUrl(), ningRequest.getHeaders(), body);
    }

    final Provider<org.asynchttpclient.Response> provider = new Provider<org.asynchttpclient.Response>() {
      private boolean aborted = false;
      private long size;

      @Override
      public ListenableFuture<org.asynchttpclient.Response> provide() {
        return asyncHttpClient.executeRequest(ningRequest, new AsyncCompletionHandler<org.asynchttpclient.Response>() {
          @Override
          public org.asynchttpclient.Response onCompleted(final org.asynchttpclient.Response response) throws Exception {
            if (aborted) {
              throw new RuntimeException("Response size is bigger than the limit: " + responseMaxSize);
            }
            return response;
          }

          @Override
          public State onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            if (responseMaxSize > 0) {
              size += content.length();
              if (size > responseMaxSize) {
                aborted = true;
                return State.ABORT;
              }
            }
            return super.onBodyPartReceived(content);
          }
        });
      }
    };

    return fromListenableFuture(provider);
  }

  private org.asynchttpclient.cookie.Cookie transformToNingCookie(final Cookie cookie) {

    return org.asynchttpclient.cookie.Cookie.newValidCookie(cookie.getName(), cookie.getValue(), false,
      cookie.getDomain(), cookie.getPath(), cookie.getMaxAge(),
      cookie.isSecure(), cookie.isHttpOnly());
  }

  /**
   * Prepares the request body - setting the body, charset and
   * content length by body type
   *
   * @throws IOException
   */
  private void prepareRequestBody() throws IOException {

    final byte[] body;
    if (bodyByteArray != null) {
      body = bodyByteArray;
    } else if (bodyString != null) {
      body = bodyString.getBytes(charset);
    } else if (bodyObject != null) {
      body = marshallingStrategy.marshall(bodyObject);
    } else {
      return;
    }

    ningRequestBuilder.setBody(body);
    ningRequestBuilder.setCharset(charset);
  }
}