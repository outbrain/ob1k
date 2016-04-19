package com.outbrain.ob1k.cache.memcache.folsom;

import com.spotify.folsom.Transcoder;
import org.apache.commons.lang.SerializationException;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * @author Eran Harel
 */
public class MessagePackTranscoder<T> implements Transcoder<T> {

  private final MessagePack messagePack;
  private final Type valueType;

  public MessagePackTranscoder(final MessagePack messagePack, final Type valueType) {
    this.messagePack = Objects.requireNonNull(messagePack, "messagePack must not be null");
    this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
  }

  @Override
  @SuppressWarnings("unchecked")
  public T decode(final byte[] b) {
    final Template<?> template = messagePack.lookup(valueType);

    try {
      final Value value = messagePack.read(b);
      return (T) template.read(new Converter(messagePack, value), null);
    } catch (final IOException e) {
      throw new SerializationException("Failed to decode to type " + valueType.getTypeName(), e);
    }
  }

  @Override
  public byte[] encode(final T t) {
    try {
      return messagePack.write(t);
    } catch (final IOException e) {
      throw new SerializationException("Failed to encode input " + t.getClass().getSimpleName() + " to type " + valueType.getTypeName(), e);
    }
  }
}
