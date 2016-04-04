package com.outbrain.ob1k.cache.memcache.folsom;

import com.spotify.folsom.Transcoder;
import org.apache.commons.lang.SerializationException;
import org.msgpack.MessagePack;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Eran Harel
 */
public class MessagePackTranscoder<T> implements Transcoder<T> {

  private final MessagePack messagePack;
  private final Class<T> valueType;

  public MessagePackTranscoder(final MessagePack messagePack, final Class<T> valueType) {
    this.messagePack = Objects.requireNonNull(messagePack, "messagePack must not be null");
    this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
  }

  @Override
  public T decode(final byte[] b) {
    try {
      return messagePack.read(b, valueType);
    } catch (final IOException e) {
      throw new SerializationException("Failed to decode to type " + valueType.getSimpleName(), e);
    }
  }

  @Override
  public byte[] encode(final T t) {
    try {
      return messagePack.write(t);
    } catch (final IOException e) {
      throw new SerializationException("Failed to encode input " + t.getClass().getSimpleName() + " to type " + valueType.getSimpleName(), e);
    }
  }
}
