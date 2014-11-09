package com.outbrain.ob1k.common.marshalling;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.outbrain.ob1k.Request;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Created with IntelliJ IDEA.
 * User: aronen
 * Date: 8/18/13
 * Time: 3:18 PM
 */
public interface RequestMarshaller {
  void registerTypes(Type... types);

  Object[] unmarshallRequestParams(Request request, Method method, String[] paramNames) throws IOException;
  FullHttpResponse marshallResponse(Object res, HttpResponseStatus status) throws IOException;
  HttpResponse marshallResponseHeaders(final boolean rawStream);
  HttpContent marshallResponsePart(Object res, HttpResponseStatus status, boolean rawStream) throws IOException;

  void marshallRequestParams(AsyncHttpClient.BoundRequestBuilder requestBuilder, Object[] requestParams) throws IOException;
  Object unmarshallResponse(Response httpResponse, Type resType, boolean failOnError) throws IOException;
}
