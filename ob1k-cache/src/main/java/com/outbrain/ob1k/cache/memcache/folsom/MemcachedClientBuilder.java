package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.folsom.MemcacheClientBuilder;
import org.msgpack.MessagePack;

/**
 * A small extension over Folsom {@link MemcachedClientBuilder} that simplifies the creation of JSON and MessagePack
 * based clients.
 * @author Eran Harel
 */
public class MemcachedClientBuilder {

  /**
   * Create a client builder for JSON values.
   * @return The builder
   */
  public static <T> MemcacheClientBuilder<T> newJsonClient(final Class<T> valueType) {
    return newJsonClient(DefaultObjectMapperHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for JSON values.
   * @return The builder
   */
  public static <T> MemcacheClientBuilder<T> newJsonClient(final ObjectMapper objectMapper, final Class<T> valueType) {
    return new MemcacheClientBuilder<>(new JsonTranscoder<>(objectMapper, valueType));
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcacheClientBuilder<T> newMessagePackClient(final Class<T> valueType) {
    return newMessagePackClient(DefaultMessagePackHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcacheClientBuilder<T> newMessagePackClient(final MessagePack messagePack, final Class<T> valueType) {
    messagePack.register(valueType);
    return new MemcacheClientBuilder<>(new MessagePackTranscoder<>(messagePack, valueType));
  }


  private static class DefaultObjectMapperHolder {
    private static final ObjectMapper INSTANCE = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

      return objectMapper;
    }
  }

  private static class DefaultMessagePackHolder {
    private static final MessagePack INSTANCE = new MessagePack();
  }
}
