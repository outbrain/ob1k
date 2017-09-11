package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ctx.ClientRequestContext;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import com.outbrain.ob1k.http.utils.UrlUtils;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author marenzon
 */
class ClientEndpointRequestBuilder {

  public static RequestBuilder build(final HttpClient httpClient, final EndpointDescription endpointDescription, final ClientRequestContext ctx,
                                     final MarshallingStrategy marshallingStrategy) throws IOException, EncoderException {

    final RequestBuilder requestBuilder = getRequestBuilder(endpointDescription, httpClient, ctx);
    final Object[] requestValues = setPathParamsFromValues(requestBuilder, ctx);

    requestBuilder.setMarshallingStrategy(marshallingStrategy);
    requestBuilder.setBody(requestValues);
    requestBuilder.setContentType(endpointDescription.getContentType());

    return requestBuilder;
  }

  private static Object[] setPathParamsFromValues(final RequestBuilder requestBuilder, final ClientRequestContext ctx)
          throws EncoderException {

    final Object[] params = ctx.getParams();

    if (params == null) {
      return new Object[0];
    }

    final List<Object> requestValues = new LinkedList<>(Arrays.asList(params));
    final List<String> pathParams = UrlUtils.extractPathParams(ctx.getUrl());

    for (final String pathParam : pathParams) {
      final Object paramValue = requestValues.remove(0);
      requestBuilder.setPathParam(pathParam, String.valueOf(paramValue));
    }

    return requestValues.toArray();
  }

  private static RequestBuilder getRequestBuilder(final EndpointDescription endpointDescription, final HttpClient httpClient,
                                                  final ClientRequestContext ctx) {

    final RequestBuilder requestBuilder;
    final HttpRequestMethodType requestMethodType = endpointDescription.getRequestMethodType();

    switch (requestMethodType) {
      case GET:
        requestBuilder = httpClient.get(ctx.getUrl());
        break;
      case PUT:
        requestBuilder = httpClient.put(ctx.getUrl());
        break;
      case DELETE:
        requestBuilder = httpClient.delete(ctx.getUrl());
        break;
      case OPTIONS:
        requestBuilder = httpClient.options(ctx.getUrl());
        break;
      default:
        requestBuilder = httpClient.post(ctx.getUrl());
    }

    return requestBuilder;
  }
}
