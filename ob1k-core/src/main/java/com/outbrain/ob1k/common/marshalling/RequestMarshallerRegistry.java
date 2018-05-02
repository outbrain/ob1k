package com.outbrain.ob1k.common.marshalling;

import com.fasterxml.jackson.databind.Module;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.outbrain.ob1k.http.common.ContentType.JSON;
import static com.outbrain.ob1k.http.common.ContentType.MESSAGE_PACK;
import static com.outbrain.ob1k.http.common.ContentType.TEXT_PLAIN;
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
    RequestMarshaller jsonMarsjhaller = marshallers.get(JSON.requestEncoding());
    RequestMarshaller requestMarshaller = null;
    if (contentType != null) {
      requestMarshaller = marshallers.get(normalizeContentType(contentType));
    }

    if (requestMarshaller == null) {
      return jsonMarsjhaller;
    }

    return requestMarshaller;
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
      marshallers.put(JSON.requestEncoding(), new JsonRequestMarshaller());
      marshallers.put(TEXT_PLAIN.requestEncoding(), new JsonRequestMarshaller());
    }

    public Builder withMessagePack() {
      marshallers.put(MESSAGE_PACK.requestEncoding(), new MessagePackRequestMarshaller());
      return this;
    }

    public Builder withCustoms(final Map<String, RequestMarshaller> customMarshaller) {
      marshallers.putAll(requireNonNull(customMarshaller));
      return this;
    }

    public Builder withJsonModules(final Module... modules) {
      marshallers.put(JSON.requestEncoding(), new JsonRequestMarshaller().withModules(modules));
      return this;
    }

    public RequestMarshallerRegistry build() {
      return new RequestMarshallerRegistry(marshallers);
    }
  }
}
