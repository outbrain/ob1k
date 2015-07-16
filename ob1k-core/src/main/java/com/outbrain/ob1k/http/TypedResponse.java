package com.outbrain.ob1k.http;

import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;

import java.io.IOException;

/**
 * Provides typed response, which contains unmarshalled response of T
 * by the {@link MarshallingStrategy} strategy provided.
 *
 * The unmarshalling is lazy, and occurs only when calling to {@link TypedResponse#getTypedBody()}
 *
 * @see MarshallingStrategy
 * @see RequestBuilder#asTypedResponse(Class)
 * @see RequestBuilder#asValue(Class)
 * @see RequestBuilder#asTypedStream(Class)
 * @see RequestBuilder#asStreamValue(Class)
 * @author marenzon
 */
public interface TypedResponse<T> extends Response {

  T getTypedBody() throws IOException;
}