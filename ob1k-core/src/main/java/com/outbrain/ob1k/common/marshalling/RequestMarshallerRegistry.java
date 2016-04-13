package com.outbrain.ob1k.common.marshalling;

import java.util.HashMap;
import java.util.Map;

import static com.outbrain.ob1k.http.common.ContentType.JSON;
import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;

/**
 * @author aronen
 */
public class RequestMarshallerRegistry {

  public final static RequestMarshallerRegistry INSTANCE = new RequestMarshallerRegistry();

  private final Map<String, RequestMarshaller> marshallers;

  private RequestMarshallerRegistry() {
    marshallers = new HashMap<>();
    marshallers.put(JSON.requestEncoding(), new JsonRequestMarshaller());
    marshallers.put(MESSAGE_PACK.requestEncoding(), new MessagePackRequestMarshaller());
  }

  public RequestMarshaller getMarshaller(final String contentType) {
    if (marshallers.containsKey(contentType)) {
      return marshallers.get(contentType);
    }

    // default json marshaller, if unknown content type
    return marshallers.get(JSON.requestEncoding());
  }
}
