package com.outbrain.ob1k.client.http;

import com.ning.http.client.AsyncHttpClient;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author aronen
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

  public AsyncHttpClient.BoundRequestBuilder buildHeadRequest(final AsyncHttpClient client, final String url) {
    return client.prepareHead(url);
  }

  public AsyncHttpClient.BoundRequestBuilder buildPutRequest(final AsyncHttpClient client, final String url, final String body, final String contentType) throws UnsupportedEncodingException {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePut(url);
    requestBuilder.setBody(body);
    requestBuilder.setContentLength(body.getBytes("UTF8").length);
    requestBuilder.setBodyEncoding("UTF8");
    requestBuilder.setHeader("Content-Type", contentType);
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildPostRequestWithParams(final AsyncHttpClient client, final String url,
                                                                        final String contentType, final Object[] requestParams) throws IOException {
    final AbstractMap.SimpleEntry<String, Object[]> urlAndParams = replacePathParamsWithValues(url, requestParams);
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePost(urlAndParams.getKey());
    final RequestMarshaller marshaller = registry.getMarshaller(contentType);
    marshaller.marshallRequestParams(requestBuilder, urlAndParams.getValue());
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildGetRequest(final AsyncHttpClient client, final String url, final HttpClient.Param... requestParams) {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);

    if (requestParams != null) {
      for (final HttpClient.Param param : requestParams) {
        requestBuilder.addQueryParam(param.key, param.value);
      }
    }

    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildGetRequestWithParams(final AsyncHttpClient client, final String url,
                                                                       final Object[] ctxParams, final String contentType) throws IOException {
    final AbstractMap.SimpleEntry<String, Object[]> urlAndParams = replacePathParamsWithValues(url, ctxParams);
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(urlAndParams.getKey());
    requestBuilder.setHeader("Content-Type", contentType);
    final RequestMarshaller marshaller = registry.getMarshaller(contentType);
    marshaller.marshallRequestParams(requestBuilder, urlAndParams.getValue());
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildGetRequest(final AsyncHttpClient client, final String url, final Map<String, String> requestParams) {
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareGet(url);
    if (requestParams != null) {
      for (final Map.Entry<String, String> param : requestParams.entrySet()) {
        requestBuilder.addQueryParam(param.getKey(), param.getValue());
      }
    }

    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildPutRequestWithParams(final AsyncHttpClient client, final String url,
                                                                        final String contentType, final Object[] requestParams) throws IOException {
    final AbstractMap.SimpleEntry<String, Object[]> urlAndParams = replacePathParamsWithValues(url, requestParams);
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.preparePut(urlAndParams.getKey());
    requestBuilder.setHeader("Content-Type", contentType);
    final RequestMarshaller marshaller = registry.getMarshaller(contentType);
    marshaller.marshallRequestParams(requestBuilder, urlAndParams.getValue());
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder buildDeleteRequestWithParams(final AsyncHttpClient client, final String url,
                                                                          final Object[] ctxParams, final String contentType) throws IOException {
    final AbstractMap.SimpleEntry<String, Object[]> urlAndParams = replacePathParamsWithValues(url, ctxParams);
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = client.prepareDelete(urlAndParams.getKey());
    requestBuilder.setHeader("Content-Type", contentType);
    final RequestMarshaller marshaller = registry.getMarshaller(contentType);
    marshaller.marshallRequestParams(requestBuilder, urlAndParams.getValue());
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

  private AbstractMap.SimpleEntry<String, Object[]> replacePathParamsWithValues(final String url, final Object[] params) {
    if (params == null) {
      return new AbstractMap.SimpleEntry<>(url, null);
    }
    final List<Object> paramsList = new LinkedList<>(Arrays.asList(params));
    int index = url.indexOf('{');
    int pathIndex = 0;
    String newUrl = url;
    while (index >= 0) {
      paramsList.remove(0);
      final int endIndex = newUrl.indexOf('}', index);
      final String pathParameter = newUrl.substring(index, endIndex + 1);
      newUrl = newUrl.replace(pathParameter, String.valueOf(params[pathIndex]));
      index = newUrl.indexOf('{');
      pathIndex++;
    }
    return new AbstractMap.SimpleEntry<>(newUrl, paramsList.toArray());
  }
}