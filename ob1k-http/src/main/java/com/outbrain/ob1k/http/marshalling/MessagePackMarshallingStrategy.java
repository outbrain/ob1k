package com.outbrain.ob1k.http.marshalling;

import com.outbrain.ob1k.http.Response;

import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;
import org.msgpack.template.Template;
import org.msgpack.type.Value;
import org.msgpack.unpacker.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * MessagePack unmarshalling strategy implementation
 *
 * @author marenzon
 */
public class MessagePackMarshallingStrategy implements MarshallingStrategy {

  private static final Logger log = LoggerFactory.getLogger(MessagePackMarshallingStrategy.class);
  private static final int NO_CONTENT = 204;
  private final MessagePack messagePack;

  public MessagePackMarshallingStrategy(final MessagePack messagePack) {

    this.messagePack = messagePack;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T unmarshall(final Type type, final Response response) throws IOException {

    final int statusCode = response.getStatusCode();

    if (statusCode < 200 || statusCode >= 300) {
      log.debug("request fail, status code: {}", statusCode, response);
      throw new IOException("Call failed for url: " + response.getUrl() + ", status code: " + statusCode + ".\n" +
              response.getResponseBody());
    }

    if (NO_CONTENT == statusCode || !response.hasResponseBody()) {
      // on empty body the object mapper throws "JsonMappingException: No content to map due to end-of-input"
      return null;
    }

    final Value value = messagePack.read(response.getResponseBodyAsBytes());
    final Template<T> template = (Template<T>) messagePack.lookup(type);
    return template.read(new Converter(messagePack, value), null);
  }

  @Override
  public byte[] marshall(final Object value) throws IOException {

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final Packer packer = messagePack.createPacker(outputStream);

    packer.write(value);

    return outputStream.toByteArray();
  }
}
