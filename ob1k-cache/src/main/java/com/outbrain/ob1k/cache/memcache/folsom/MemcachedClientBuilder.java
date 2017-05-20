package com.outbrain.ob1k.cache.memcache.folsom;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outbrain.ob1k.cache.memcache.compression.CompressionAlgorithm;
import com.outbrain.ob1k.cache.memcache.compression.Compressor;
import com.outbrain.swinfra.metrics.api.MetricFactory;
import com.spotify.folsom.MemcacheClientBuilder;
import com.spotify.folsom.Transcoder;
import org.msgpack.core.MessagePack;
import org.msgpack.jackson.dataformat.JsonArrayFormat;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.jackson.dataformat.MessagePackSerializerFactory;

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
    return newObjectMapperClient(DefaultObjectMapperHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for JSON values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newJsonClient(final JavaType valueType) {
    return newObjectMapperClient(DefaultObjectMapperHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final Class<T> valueType) {
    return newObjectMapperClient(DefaultMessagePackHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final JavaType valueType) {
    return newObjectMapperClient(DefaultMessagePackHolder.INSTANCE, valueType);
  }

  /**
   * Create a client builder for MessagePack values.
   * @deprecated please use either {@link #newMessagePackClient(Class)} or {@link #newMessagePackClient(JavaType)}
   * @return The builder
   */
  @Deprecated
  public static <T> MemcachedClientBuilder<T> newMessagePackClient(final Type valueType) {
    return newClient(new JacksonTranscoder<>(DefaultMessagePackHolder.INSTANCE, valueType));
  }

  /**
   * Create a new client builder for given an ObjectMapper instance, and class of value
   *
   * @param objectMapper object mapper for serialization values
   * @param valueType values type
   * @param <T> values type parameter
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newObjectMapperClient(final ObjectMapper objectMapper,
                                                                    final Class<T> valueType) {
    return newClient(new JacksonTranscoder<>(objectMapper, valueType));
  }

  /**
   * Create a new client builder for given an ObjectMapper instance, and {@link JavaType} of value
   *
   * @param objectMapper object mapper for serialization values
   * @param valueType values type
   * @param <T> values type parameter
   * @return The builder
   */
  public static <T> MemcachedClientBuilder<T> newObjectMapperClient(final ObjectMapper objectMapper,
                                                                    final JavaType valueType) {
    return newClient(new JacksonTranscoder<>(objectMapper, valueType));
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

  public MemcachedClientBuilder<T> withCompression(final CompressionAlgorithm algorithm) {
    return withCompression(algorithm.getCompressor());
  }

  public MemcachedClientBuilder<T> withCompression(final Compressor compressor) {
    compressionApplier = (transcoder) -> new CompressionTranscoder<>(transcoder, compressor);
    return this;
  }

  public MemcacheClientBuilder<T> build() {
    return new MemcacheClientBuilder<>(objectSizeMonitoringApplier.compose(compressionApplier).apply(transcoder));
  }

  static class DefaultObjectMapperHolder {
    static final ObjectMapper INSTANCE = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
      final ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

      return objectMapper;
    }
  }

  static class DefaultMessagePackHolder {
    static final ObjectMapper INSTANCE = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
      final MessagePack.PackerConfig config = new MessagePack.PackerConfig().withStr8FormatSupport(false);
      final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory(config));

      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      objectMapper.setAnnotationIntrospector(new JsonArrayFormat());
      objectMapper.setSerializerFactory(new MessagePackSerializerFactory());

      return objectMapper;
    }
  }
}
