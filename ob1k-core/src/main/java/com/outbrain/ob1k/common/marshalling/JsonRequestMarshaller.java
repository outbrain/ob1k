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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.base.Strings;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import com.outbrain.ob1k.HttpRequestMethodType;
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
        // if the method is not expecting anything, no reason trying unmarshalling
        if (paramNames.length == 0) {
            return new Object[0];
        }

        final HttpRequestMethodType httpMethod = request.getMethod();
        if (HttpRequestMethodType.GET == httpMethod || HttpRequestMethodType.DELETE == httpMethod) {
            // if we're having query params, we'll try to unmarshall by them
            // else, trying to read the values from the body
            if (!request.getQueryParams().isEmpty()) {
                return parseURLRequestParams(request, method, paramNames);
            }
        }
        final String body = request.getRequestBody();
        final Map<String, String> pathParams = request.getPathParams();
        if (body.isEmpty() && pathParams.isEmpty()) {
            return new Object[paramNames.length];
        }
        return parseBodyRequestParams(body, paramNames, pathParams, method);
    }

    @Override
    public void marshallRequestParams(final AsyncHttpClient.BoundRequestBuilder requestBuilder, final Object[] requestParams) throws IOException {
        // requests can come from a regular httpClient post request with a single param that get wrapped inside an array
        // or in case of a real RPC call with a single param. in both cases we unwrap it and send it as is.
        // the code in unmarshallRequestParams() know how to deal with both single object or array of objects.
        final Object params = requestParams == null ? new Object[0] :
            requestParams.length == 1 ? requestParams[0] : requestParams;

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
        if (HttpResponseStatus.NO_CONTENT.code() == statusCode || Strings.isNullOrEmpty(body)) {
            // on empty body the object mapper throws "JsonMappingException: No content to map due to end-of-input"
            return null;
        } else if (!failOnError || (statusCode >= 200 && statusCode < 300)) {
            return mapper.readValue(body, getJacksonType(resType));
        } else {
            throw new IOException("called failed for:" + httpResponse + "\n" + body);
        }
    }

    private Object[] parseURLRequestParams(final Request request, final Method method, final String[] paramNames) throws IOException {
        final Object[] result = new Object[paramNames.length];
        final Type[] types = method.getGenericParameterTypes();
        int index = 0;
        for (final String paramName : paramNames) {
            String param = request.getQueryParam(paramName);
            if (param == null) {
                param = request.getPathParam(paramName);
            }
            final Type currentType = types[index];
            if (param == null) {
                if (currentType instanceof Class && ((Class) currentType).isPrimitive()) {
                    throw new IOException("Parameter " + paramName + " is primitive and cannot be null");
                }
                result[index] = null;
            } else if (currentType == String.class && !param.startsWith("'") && !param.endsWith("'")) {
                // parsing is unneeded.
                result[index] = param;
            } else {
                final Object value = mapper.readValue(param, getJacksonType(types[index]));
                result[index] = value;
            }
            index++;
        }

        return result;
    }

    private Object[] parseBodyRequestParams(final String json, final String[] paramNames,
                                            final Map<String, String> pathParams, final Method method) throws IOException {
        final Type[] types = method.getGenericParameterTypes();
        final List<Object> results = new ArrayList<>(types.length);

        int index = 0;
        for (final String paramName: paramNames) {
            if (pathParams.containsKey(paramName)) {
                results.add(ParamMarshaller.unmarshall(pathParams.get(paramName), (Class) types[index]));
                index++;
            } else {
                break;
            }
        }

        if (results.size() < pathParams.size()) {
            throw new IOException("path params should be bounded to be a prefix of the method parameters list.");
        }

        if (results.size() == types.length) {
            return results.toArray();
        }

        final int numOfBodyParams = types.length - results.size();
        if (numOfBodyParams == 1) {
            // in case of single body param we assume a single object with no wrapping array.
            // we read it completely and finish.
            final Object param = mapper.readValue(json, getJacksonType(types[index]));
            results.add(param);
        } else if (numOfBodyParams > 1) {
            final JsonParser jp = factory.createParser(json);
            JsonToken token = jp.nextToken();
            if (token == JsonToken.START_ARRAY) {
                token = jp.nextToken();
                while (true) {
                    if (token == JsonToken.END_ARRAY)
                        break;

                    final Object res = mapper.readValue(jp, getJacksonType(types[index]));
                    results.add(res);
                    index++;

                    token = jp.nextToken();
                }
            } else {
                // we have multiple objects to unmarshall and no array of objects.
                throw new IOException("can't unmarshall request. got a single object in the body but expected multiple objects in an array");
            }
        }

        return results.toArray();
    }

    private JavaType getJacksonType(final Type type) {
        final TypeFactory typeFactory = TypeFactory.defaultInstance();
        return typeFactory.constructType(type);
    }

}