package com.outbrain.ob1k.common.marshalling;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.TRANSFER_ENCODING;
import static io.netty.handler.codec.http.HttpHeaders.Values.CHUNKED;
import static io.netty.handler.codec.http.HttpHeaders.Values.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.outbrain.ob1k.Request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.CharsetUtil;

/**
 * Created with IntelliJ IDEA.
 * User: aronen
 * Date: 8/18/13
 * Time: 3:22 PM
 */
public class JsonRequestMarshaller implements RequestMarshaller {
  private final ObjectMapper mapper;
  private final JsonFactory factory;

  public JsonRequestMarshaller() {
    this.factory = new JsonFactory();
    this.mapper = new ObjectMapper(factory);
    mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  @Override
  public void registerTypes(final Type... types) {
    //no need to register types.
  }

  @Override
  public Object[] unmarshallRequestParams(final Request request, final Method method, final String[] paramNames) throws IOException {
    final String httpMethod = request.getMethod();
    if (httpMethod.equals("GET") || httpMethod.equals("DELETE")) {
      return parseURLRequestParams(request, method, paramNames);
    } else if (httpMethod.equals("POST") || httpMethod.equals("PUT")) {
      final String body = request.getRequestBody();
      if (body == null || body.isEmpty()) {
        return new Object[0];
      }

      return parseBodyRequestParams(body, method);
    } else {
      throw new IllegalArgumentException("http method not supported.");
    }
  }

  @Override
  public void marshallRequestParams(final AsyncHttpClient.BoundRequestBuilder requestBuilder, final Object[] requestParams) throws IOException {
    // requests can come from a regular httpClient post request with a single param that get wrapped inside an array
    // or in case of a real RPC call with a single param. in both cases we unwrap it and send it as is.
    // the code in unmarshalRequestParams() know how to deal with both single object or array of objects.
    final Object params = (requestParams != null && requestParams.length == 1) ? requestParams[0] : requestParams;
    final String body = mapper.writeValueAsString(params);
    requestBuilder.setBody(body);
    requestBuilder.setContentLength(body.getBytes("UTF8").length);
    requestBuilder.setBodyEncoding("UTF8");
    requestBuilder.setHeader("Content-Type", ContentType.JSON.requestEncoding());
  }

  @Override
  public HttpContent marshallResponsePart(final Object res, final HttpResponseStatus status, final boolean rawStream) throws IOException {
    final String content = rawStream ?
        mapper.writeValueAsString(res) + "<br/>\n" :
        ChunkHeader.ELEMENT_HEADER + mapper.writeValueAsString(res) + "\n";

    final ByteBuf buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
    return new DefaultHttpContent(buf);
  }

  @Override
  public FullHttpResponse marshallResponse(final Object res, final HttpResponseStatus status) throws JsonProcessingException {
    final String content = mapper.writeValueAsString(res);
    final ByteBuf buf = Unpooled.copiedBuffer(content, CharsetUtil.UTF_8);
    final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buf);

    response.headers().set(CONTENT_TYPE, ContentType.JSON.responseEncoding());
    return response;
  }

  @Override
  public HttpResponse marshallResponseHeaders(final boolean rawStream) {
    final HttpResponse res = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
    res.headers().add(TRANSFER_ENCODING, CHUNKED);
    res.headers().add(CONNECTION, KEEP_ALIVE);
    res.headers().add(CONTENT_TYPE, rawStream ? ContentType.TEXT_HTML.responseEncoding() : ContentType.JSON.responseEncoding());

    return res;
  }

  @Override
  public Object unmarshallResponse(final Response httpResponse, final Type resType, final boolean failOnError) throws IOException {
    final String body = httpResponse.getResponseBody();
    final int statusCode = httpResponse.getStatusCode();
    if (HttpResponseStatus.NO_CONTENT.code() == statusCode) {
      return null;
    } else if (!failOnError || (statusCode >= 200 && statusCode < 300)) {
      return mapper.readValue(body, getJacksonType(resType));
    } else {
      throw new IOException("called failed for:" + httpResponse + "\n" + body);
    }
  }

  private Object[] parseURLRequestParams(final Request request, final Method method, final String[] paramNames) throws IOException {
    final Object[] result = new Object[paramNames.length];

    int index = 0;
    for (final String paramName: paramNames) {
      String param = request.getQueryParam(paramName);
      if (param == null) {
        param = request.getPathParam(paramName);
      }

      if (param == null) {
        //probably a bad request. will cause a NPE downstream.
        result[index] = null;
      } else if (method.getGenericParameterTypes()[index] == String.class && !param.startsWith("'") && !param.endsWith("'")) {
        // parsing is unneeded.
        result[index] = param;
      } else {
        final Object value = mapper.readValue(param, getJacksonParamType(method, index));
        result[index] = value;
      }
      index++;
    }

    return result;
  }

  private Object[] parseBodyRequestParams(final String json, final Method method) throws IOException {
    final JsonParser jp = factory.createParser(json);
    JsonToken token;
    final List<Object> results = new ArrayList<>();
    token = jp.nextToken();
    if (token == JsonToken.START_ARRAY) {
      int index = 0;
      token = jp.nextToken();
      while (true) {
        if (token == JsonToken.END_ARRAY)
          break;

        final Object res = mapper.readValue(jp, getJacksonParamType(method, index));
        results.add(res);
        index++;

        token = jp.nextToken();
      }
    } else {
      // string contains just a single object read it completely and finish.
      final Object param = mapper.readValue(json, getJacksonParamType(method, 0));
      results.add(param);
    }

    return results.toArray();
  }

  private JavaType getJacksonParamType(final Method method, final int index) {
    final Type[] types = method.getGenericParameterTypes();
    return getJacksonType(types[index]);
  }

  private JavaType getJacksonType(final Type type) {
    final TypeFactory typeFactory = TypeFactory.defaultInstance();
    return typeFactory.constructType(type);
  }

}
