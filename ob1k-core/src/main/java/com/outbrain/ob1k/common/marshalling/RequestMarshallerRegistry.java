package com.outbrain.ob1k.common.marshalling;

import java.util.HashMap;
import java.util.Map;

import static com.outbrain.ob1k.http.common.ContentType.JSON;
import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;

/**
 * @author aronen
 */
public class RequestMarshallerRegistry {

  private final Map<String, RequestMarshaller> marshallers;

  public RequestMarshallerRegistry() {
    marshallers = new HashMap<>();
    marshallers.put(JSON.requestEncoding(), new JsonRequestMarshaller());
    marshallers.put(MESSAGE_PACK.requestEncoding(), new MessagePackRequestMarshaller());
  }

  public RequestMarshaller getMarshaller(final String contentType) {
    RequestMarshaller res = marshallers.get(contentType);
    if (res == null) {
      res = marshallers.get(JSON.requestEncoding());
    }

    return res;
  }
}
