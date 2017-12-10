package com.outbrain.ob1k.common.marshalling;

import com.outbrain.ob1k.http.common.ContentType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * @author aronen
 */
public class RequestMarshallerRegistry {

  private final Map<String, RequestMarshaller> marshallers;

  public RequestMarshallerRegistry(final Map<String, RequestMarshaller> marshallers) {
    this.marshallers = requireNonNull(marshallers);
  }

  public RequestMarshallerRegistry() {
    this.marshallers = new HashMap<>();
    this.marshallers.put(ContentType.JSON.requestEncoding(), new JsonRequestMarshaller());
    this.marshallers.put(ContentType.MESSAGE_PACK.requestEncoding(), new MessagePackRequestMarshaller());
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
