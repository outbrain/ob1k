package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.folsom.Transcoder;
import org.apache.commons.lang.SerializationException;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Eran Harel
 */
public class JsonTranscoder<T> implements Transcoder<T> {

  private final ObjectMapper objectMapper;
  private final Class<T> valueType;

  public JsonTranscoder(final ObjectMapper objectMapper, final Class<T> valueType) {
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
  }

  @Override
  public T decode(final byte[] b) {
    try {
      return objectMapper.readValue(b, valueType);
    } catch (final IOException e) {
      throw new SerializationException("Failed to decode to type " + valueType.getSimpleName(), e);
    }
  }

  @Override
  public byte[] encode(final T t) {
    try {
      return objectMapper.writeValueAsBytes(t);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Failed to encode input " + t.getClass().getSimpleName() + " to type " + valueType.getSimpleName(), e);
    }
  }
}
