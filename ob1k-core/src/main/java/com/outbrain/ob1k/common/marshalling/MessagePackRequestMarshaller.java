package com.outbrain.ob1k.common.marshalling;

import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;

import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;

/**
 * @author marenzon
 */
public class MessagePackRequestMarshaller extends JacksonRequestMarshaller {

  public MessagePackRequestMarshaller() {
    super(new MessagePackFactory(), MESSAGE_PACK);
  }

  @Override
  public byte[] marshallRequestParams(final Object[] requestParams) throws IOException {
    // Not sending simple object in case of MessagePack because previous implementation
    // have sent always array of objects, even in case of single object. So it's backward-compatible.
    final Object params = requestParams == null ? new Object[0] : requestParams;
    return marshallingStrategy.marshall(params);
  }
}