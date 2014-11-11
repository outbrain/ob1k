package com.outbrain.ob1k.client.http;

import com.ning.http.client.*;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * User: aronen
 * Date: 6/16/13
 * Time: 6:06 PM
 */
public class RequestBuilder {
  private final RequestMarshallerRegistry registry;

  public RequestBuilder(final RequestMarshallerRegistry registry) {
    this.registry = registry;
  }

  public AsyncHttpClient.BoundRequestBuilder buildPostRequest(final AsyncHttpClient client, final String url, final String body, final String contentType) throws UnsupportedEncodingException {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(url);
    requestBuilder.setBody(body);
    requestBuilder.setContentLength(body.getBytes("UTF8").length);
    requestBuilder.setBodyEncoding("UTF8");
    requestBuilder.setHeader("Content-Type", contentType);
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildPostRequestWithParams(final AsyncHttpClient client, final String url,
                                                                        final String contentType, final Object[] requestParams) throws IOException {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(url);

    final RequestMarshaller marshaller = registry.getMarshaller(contentType);
    marshaller.marshallRequestParams(requestBuilder, requestParams);
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildGetRequest(final AsyncHttpClient client, final String url, final HttpClient.Param... requestParams) {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);

    if (requestParams != null) {
      for (final HttpClient.Param param : requestParams) {
        requestBuilder.addQueryParameter(param.key, param.value);
      }
    }

    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildGetRequest(final AsyncHttpClient client, final String url, final Map<String, String> requestParams) {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);
    if (requestParams != null) {
      for (final Map.Entry<String, String> param : requestParams.entrySet()) {
        requestBuilder.addQueryParameter(param.getKey(), param.getValue());
      }
    }

    return requestBuilder;
  }

  public <T> T unmarshallResponse(final com.ning.http.client.Response httpResponse, final Class<T> resType) throws IOException {
    return unmarshallResponse(httpResponse, resType, true);
  }

  public <T> T unmarshallResponse(final com.ning.http.client.Response httpResponse, final Class<T> resType, final boolean failOnError) throws IOException {
    final RequestMarshaller marshaller = registry.getMarshaller(httpResponse.getContentType());
    @SuppressWarnings("unchecked")
    final T result = (T) marshaller.unmarshallResponse(httpResponse, resType, failOnError);
    return result;
  }

  public Object unmarshallResponse(final com.ning.http.client.Response httpResponse, final Type resType) throws IOException {
    final RequestMarshaller marshaller = registry.getMarshaller(httpResponse.getContentType());
    return marshaller.unmarshallResponse(httpResponse, resType, true);
  }

}
