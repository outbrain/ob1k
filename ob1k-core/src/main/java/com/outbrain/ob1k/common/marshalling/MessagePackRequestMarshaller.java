package com.outbrain.ob1k.common.marshalling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.msgpack.core.MessagePack;
import org.msgpack.jackson.dataformat.JsonArrayFormat;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.msgpack.jackson.dataformat.MessagePackSerializerFactory;

import java.io.IOException;

import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;

/**
 * @author marenzon
 */
public class MessagePackRequestMarshaller extends JacksonRequestMarshaller {

  public MessagePackRequestMarshaller() {
    super(createObjectMapper(), MESSAGE_PACK);
  }

  @Override
  public byte[] marshallRequestParams(final Object[] requestParams) throws IOException {
    // Not sending simple object in case of MessagePack because previous implementation
    // have sent always array of objects, even in case of single object. So it's backward-compatible.
    return marshallingStrategy.marshall(requestParams);
  }

  private static ObjectMapper createObjectMapper() {
    final MessagePack.PackerConfig config = new MessagePack.PackerConfig().withStr8FormatSupport(false);
    final ObjectMapper objectMapper = new ObjectMapper(new MessagePackFactory(config));

    objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.setAnnotationIntrospector(new JsonArrayFormat());
    objectMapper.setSerializerFactory(new MessagePackSerializerFactory());

    return objectMapper;
  }
}