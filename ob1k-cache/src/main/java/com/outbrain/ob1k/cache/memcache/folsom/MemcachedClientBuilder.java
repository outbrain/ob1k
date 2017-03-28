package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbrain.ob1k.cache.memcache.compression.CompressionAlgorithm;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.spotify.folsom.MemcacheClientBuilder;
import com.spotify.folsom.Transcoder;
import org.apache.commons.lang3.ClassUtils;
import org.msgpack.MessagePack;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.function.Function;

/**
 * A small extension over Folsom {@link MemcachedClientBuilder} that simplifies the creation of JSON and MessagePack
 * based clients.
 * @author Eran Harel
 */
public class MemcachedClientBuilder<T> {

  private Transcoder<T> transcoder;
  private Function<Transcoder<T>, Transcoder<T>> objectSizeMonitoringApplier = Function.identity();
  private Function<Transcoder<T>, Transcoder<T>> compressionApplier = Function.identity();

  private MemcachedClientBuilder(final Transcoder<T> transcoder) {
    this.transcoder = Objects.requireNonNull(transcoder, "transcoder must not be null");
  }

  /**
   * Create a client builder for JSON values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newJsonClient(final Class<T> valueType) {
    return newJsonClient(DefaultObjectMapperHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for JSON values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newJsonClient(final ObjectMapper objectMapper, final Class<T> valueType) {
    return newClient(new JsonTranscoder<>(objectMapper, valueType));
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final Class<T> valueType) {
    return newMessagePackClient(DefaultMessagePackHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final MessagePack messagePack, final Class<T> valueType) {
    if (!ClassUtils.isPrimitiveOrWrapper(valueType)) {
      messagePack.register(valueType);
    }
    return newClient(new MessagePackTranscoder<>(messagePack, valueType));
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final Type valueType) {
    return newMessagePackClient(DefaultMessagePackHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final MessagePack messagePack, final Type valueType) {
    return newClient(new MessagePackTranscoder<>(messagePack, valueType));
  }

  /**
   * Create a client builder
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newClient(final Transcoder<T> transcoder) {
    return new MemcachedClientBuilder<>(transcoder);
  }

  public MemcachedClientBuilder<T> withObjectSizeMonitoring(final MetricFactory metricFactory, final String cacheName, final long sampleRate) {
    objectSizeMonitoringApplier = (transcoder) -> new ObjectSizeMonitoringTranscoder<>(transcoder, metricFactory, cacheName, sampleRate);
    return this;
  }

  public MemcachedClientBuilder<T> withCompression() {
    compressionApplier = (transcoder) -> new CompressionTranscoder<T>(transcoder, CompressionAlgorithm.getDefault());
    return this;
  }

  public MemcachedClientBuilder<T> withCompression(CompressionAlgorithm compressionAlgorithm) {
    compressionApplier = (transcoder) -> new CompressionTranscoder<T>(transcoder, compressionAlgorithm.getInstance());
    return this;
  }

  public MemcacheClientBuilder<T> build() {
    return new MemcacheClientBuilder<>(objectSizeMonitoringApplier.compose(compressionApplier).apply(transcoder));
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
