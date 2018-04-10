package com.outbrain.ob1k.http;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.common.Header;
import com.outbrain.ob1k.http.common.Param;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.EncoderException;
import rx.Observable;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * The HttpClient request builder.
 * Provides setter for defining of how the request would look,
 * with the execution methods.
 *
 * Can be re-usable for more than one execution
 *
 * @author marenzon
 */
public interface RequestBuilder {

  String USER_AGENT_HEADER = "User-Agent";
  String CONTENT_TYPE_HEADER = "Content-Type";
  Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;

  RequestBuilder setContentType(ContentType contentType);

  RequestBuilder setContentType(String contentType);

  RequestBuilder setPathParam(String param, String value) throws EncoderException;

  RequestBuilder setPathParam(Param param) throws EncoderException;

  RequestBuilder setPathParams(List<Param> params) throws EncoderException;

  RequestBuilder setUserAgent(String userAgent);

  RequestBuilder addHeader(String name, final String value);

  RequestBuilder addHeader(Header header);

  RequestBuilder addHeaders(List<Header> headers);

  RequestBuilder setBody(String body);

  RequestBuilder setBody(byte[] body);

  RequestBuilder setBody(Object body);

  RequestBuilder setBodyEncoding(Charset charset);

  RequestBuilder withBasicAuth(String username, String password);

  RequestBuilder addQueryParam(String name, String value);

  RequestBuilder addQueryParams(Map<String, String> params);

  RequestBuilder addQueryParam(Param param);

  RequestBuilder addQueryParams(List<Param> params);

  RequestBuilder setResponseMaxSize(long responseMaxSize);

  RequestBuilder addCookie(Cookie cookie);

  RequestBuilder setRequestTimeout(int requestTimeout);

  RequestBuilder setMarshallingStrategy(MarshallingStrategy marshallingStrategy);

  ComposableFuture<Response> asResponse();

  Observable<Response> asStream();

  <T> ComposableFuture<TypedResponse<T>> asTypedResponse(Class<T> type);

  <T> ComposableFuture<TypedResponse<T>> asTypedResponse(Type type);

  <T> Observable<TypedResponse<T>> asTypedStream(Class<T> type);

  <T> Observable<TypedResponse<T>> asTypedStream(Type type);

  <T> ComposableFuture<T> asValue(Class<T> type);

  <T> ComposableFuture<T> asValue(Type type);

  <T> Observable<T> asStreamValue(Class<T> type);

  <T> Observable<T> asStreamValue(Type type);
}