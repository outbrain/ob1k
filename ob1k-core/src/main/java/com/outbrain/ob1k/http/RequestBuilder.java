package com.outbrain.ob1k.http;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.http.common.ContentType;
import com.outbrain.ob1k.http.common.Cookie;
import com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy;
import org.apache.commons.codec.EncoderException;
import rx.Observable;

import java.io.InputStream;
import java.lang.reflect.Type;
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

  RequestBuilder setContentType(ContentType contentType);

  RequestBuilder setContentType(String contentType);

  RequestBuilder setPathParams(Map<String, String> params) throws EncoderException;

  RequestBuilder setPathParam(String param, String value) throws EncoderException;

  RequestBuilder setUserAgent(String userAgent);

  RequestBuilder addHeader(String name, final String value);

  RequestBuilder setBody(String body);

  RequestBuilder setBody(byte[] body);

  RequestBuilder setBody(InputStream stream);

  RequestBuilder addQueryParam(String name, String value);

  RequestBuilder addCookie(Cookie cookie);

  RequestBuilder setRequestTimeout(int requestTimeout);

  RequestBuilder setUnmarshallingStrategy(UnmarshallingStrategy unmarshallingStrategy);

  ComposableFuture<Response> execute();

  Observable<Response> executeStream();

  <T> ComposableFuture<TypedResponse<T>> executeTyped(Class<T> type);

  <T> ComposableFuture<TypedResponse<T>> executeTyped(Type type);

  <T> Observable<TypedResponse<T>> executeStream(Class<T> type);

  <T> Observable<TypedResponse<T>> executeStream(Type type);
}