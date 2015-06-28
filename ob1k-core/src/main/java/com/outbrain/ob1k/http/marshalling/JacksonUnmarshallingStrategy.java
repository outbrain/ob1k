package com.outbrain.ob1k.http.marshalling;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.outbrain.ob1k.http.Response;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;

/**
 * FasterXML's Jackson unmarshalling strategy
 *
 * @author marenzon
 */
public class JacksonUnmarshallingStrategy implements UnmarshallingStrategy {

  private final ObjectMapper objectMapper;

  public JacksonUnmarshallingStrategy(final ObjectMapper objectMapper) {

    this.objectMapper = objectMapper;
  }

  public JacksonUnmarshallingStrategy() {

    this(new ObjectMapper());
  }

  @Override
  public <T> T unmarshall(final Type type, final Response response) throws IOException {

    if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
      String host;
      try {
        host = response.getUri().getHost();
      } catch (final URISyntaxException e) {
        host = "unknown (failed parsing)";
      }
      throw new IOException("Call failed for host: " + host + ", status code: " + response.getStatusCode());
    }

    return objectMapper.readValue(response.getResponseBody(), getJacksonType(type));
  }

  private JavaType getJacksonType(final Type type) {

    final TypeFactory typeFactory = TypeFactory.defaultInstance();
    return typeFactory.constructType(type);
  }
}