package com.outbrain.ob1k.common.marshalling;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * User: aronen
 * Date: 8/18/13
 * Time: 3:29 PM
 */
public class RequestMarshallerRegistry {
  private Map<String, RequestMarshaller> marshallers;

  public RequestMarshallerRegistry() {
    marshallers = new HashMap<>();
    marshallers.put(ContentType.JSON.requestEncoding(), new JsonRequestMarshaller());
    final MessagePackRequestMarshaller messagePackRequestMarshaller = new MessagePackRequestMarshaller();
    marshallers.put(ContentType.MESSAGE_PACK.requestEncoding(), messagePackRequestMarshaller);
  }

  public RequestMarshaller getMarshaller(final String contentType) {
    RequestMarshaller res = marshallers.get(contentType);
    if (res == null) {
      res = marshallers.get(ContentType.JSON.requestEncoding());
    }

    return res;
  }

  public void registerTypes(final Type... types) {
    for (final RequestMarshaller marshaller : marshallers.values()) {
      marshaller.registerTypes(types);
    }
  }
}
