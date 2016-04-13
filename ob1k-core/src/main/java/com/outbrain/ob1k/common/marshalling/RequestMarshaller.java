package com.outbrain.ob1k.common.marshalling;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.http.Response;
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
  Object[] unmarshallRequestParams(Request request, Method method, String[] paramNames) throws IOException;
  FullHttpResponse marshallResponse(Object res, HttpResponseStatus status) throws IOException;
  HttpResponse marshallResponseHeaders(final boolean rawStream);
  HttpContent marshallResponsePart(Object res, HttpResponseStatus status, boolean rawStream) throws IOException;
  byte[] marshallRequestParams(Object[] requestParams) throws IOException;
  <T> T unmarshallResponse(Response response, Type type) throws IOException;
  <T> T unmarshallStreamResponse(Response response, Type type) throws IOException;
}
