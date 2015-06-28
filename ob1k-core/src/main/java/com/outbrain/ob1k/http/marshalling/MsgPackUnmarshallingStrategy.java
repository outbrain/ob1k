package com.outbrain.ob1k.http.marshalling;

import com.outbrain.ob1k.http.Response;
import org.msgpack.MessagePack;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author marenzon
 */
public class MsgPackUnmarshallingStrategy implements UnmarshallingStrategy {

  private final MessagePack messagePack;

  public MsgPackUnmarshallingStrategy(final MessagePack messagePack) {

    this.messagePack = messagePack;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unmarshall(final Type type, final Response response) throws IOException {

    final Value value = messagePack.read(response.getResponseBodyAsBytes());
    final Template<T> template = (Template<T>) messagePack.lookup(type);

    return template.read(new Converter(messagePack, value), null);
  }
}
