package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.folsom.Transcoder;
import org.apache.commons.lang.SerializationException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author Eran Harel
 */
public class JacksonTranscoder<T> implements Transcoder<T> {

  private final ObjectMapper objectMapper;
  private final JavaType valueType;

  public JacksonTranscoder(final ObjectMapper objectMapper, final Class<T> valueType) {
    this(objectMapper, (Type) valueType);
  }

  public JacksonTranscoder(final ObjectMapper objectMapper, final Type valueType) {
    this(Objects.requireNonNull(objectMapper, "objectMapper must not be null"),
      objectMapper.constructType(valueType));
  }

  public JacksonTranscoder(final ObjectMapper objectMapper, final JavaType valueType) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
  }

  @Override
  public T decode(final byte[] b) {
    try {
      return objectMapper.readValue(b, valueType);
    } catch (final IOException e) {
      throw new SerializationException("Failed to decode to type " + valueType.getTypeName(), e);
    }
  }

  @Override
  public byte[] encode(final T t) {
    try {
      return objectMapper.writeValueAsBytes(t);
    } catch (final JsonProcessingException e) {
      throw new SerializationException("Failed to encode input " + t.getClass().getSimpleName() + " to type " + valueType.getTypeName(), e);
    }
  }
}