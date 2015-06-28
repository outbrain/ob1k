package com.outbrain.ob1k.http;

import java.io.IOException;

/**
 * Provides typed response, which contains unmarshalled response of T
 * by the {@link com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy} strategy provided.
 *
 * The unmarshalling is lazy, and occurs only when calling to {@link TypedResponse#getTypedBody()}
 *
 * @see com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy
 * @see RequestBuilder#executeTyped(Class)
 * @see RequestBuilder#executeStream(Class)
 * @author marenzon
 */
public interface TypedResponse<T> extends Response {

  T getTypedBody() throws IOException;
}