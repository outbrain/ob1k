package com.outbrain.ob1k.http.marshalling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.outbrain.ob1k.http.Response;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Jackson unmarshalling strategy implementation
 *
 * @author marenzon
 */
public class JacksonMarshallingStrategy implements MarshallingStrategy {

  private static final Logger log = LoggerFactory.getLogger(JacksonMarshallingStrategy.class);

  private final ObjectMapper objectMapper;

  public JacksonMarshallingStrategy() {

    this(createDefaultObjectMapper());
  }

  public JacksonMarshallingStrategy(final ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;
  }

  @Override
  public <T> T unmarshall(final Type type, final Response response) throws IOException {

    final int statusCode = response.getStatusCode();

    if (statusCode < 200 || statusCode >= 300) {
      log.debug("request fail, status code: {}, url: {}", statusCode, response.getUrl());
      throw new IOException("Call failed for url: " + response.getUrl() + ", status code: " + statusCode + ".\n" +
              response.getResponseBody());
    }

    if (HttpResponseStatus.NO_CONTENT.code() == statusCode || !response.hasResponseBody()) {
      // on empty body the object mapper throws "JsonMappingException: No content to map due to end-of-input"
      return null;
    }

    return objectMapper.readValue(response.getResponseBodyAsBytes(), getJacksonType(type));
  }

  @Override
  public byte[] marshall(final Object value) throws IOException {

    return objectMapper.writeValueAsBytes(value);
  }

  private JavaType getJacksonType(final Type type) {

    final TypeFactory typeFactory = TypeFactory.defaultInstance();
    return typeFactory.constructType(type);
  }

  private static ObjectMapper createDefaultObjectMapper() {

    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    return objectMapper;
  }
}
