package com.outbrain.ob1k.common.marshalling;

import com.outbrain.ob1k.http.common.ContentType;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.outbrain.ob1k.http.common.ContentType.JSON;
import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;
import static java.util.Objects.requireNonNull;

/**
 * @author aronen
 */
public class RequestMarshallerRegistry {

  private final Map<String, RequestMarshaller> marshallers;

  public static RequestMarshallerRegistry createDefault() {
    return new Builder().withMessagePack().build();
  }

  private RequestMarshallerRegistry(final Map<String, RequestMarshaller> marshallers) {
    this.marshallers = marshallers;
  }

  public RequestMarshaller getMarshaller(final String contentType) {
    if (contentType == null) {
      return marshallers.get(JSON.requestEncoding());
    }

    return marshallers.get(normalizeContentType(contentType));
  }

  public void registerTypes(final Type... types) {
    for (final RequestMarshaller marshaller : marshallers.values()) {
      marshaller.registerTypes(types);
    }
  }

  private String normalizeContentType(final String contentType) {
    // removing extra meta-data such as charset
    return contentType.split(";")[0];
  }

  public static class Builder {

    private final Map<String, RequestMarshaller> marshallers = new HashMap<>();

    public Builder() {
      // we always want to have a JSON default marshalling implementation
      for (ContentType contentType : ContentType.values()) {
        marshallers.put(contentType.requestEncoding(), new JsonRequestMarshaller());
      }
      marshallers.remove(MESSAGE_PACK.requestEncoding());
    }

    public Builder withMessagePack() {
      marshallers.put(MESSAGE_PACK.requestEncoding(), new MessagePackRequestMarshaller());
      return this;
    }

    public Builder withCustoms(final Map<String, RequestMarshaller> customMarshaller) {
      marshallers.putAll(requireNonNull(customMarshaller));
      return this;
    }

    public RequestMarshallerRegistry build() {
      return new RequestMarshallerRegistry(marshallers);
    }
  }
}
