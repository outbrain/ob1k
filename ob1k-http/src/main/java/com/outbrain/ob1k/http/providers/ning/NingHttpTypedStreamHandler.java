package com.outbrain.ob1k.http.providers.ning;

import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.netty.NettyResponse;
import rx.Observer;

import java.io.IOException;
import java.lang.reflect.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

/**
 * @param <T>
 * @author aronen, marenzon
 */
class NingHttpTypedStreamHandler<T> extends NingHttpStreamHandler<TypedResponse<T>> {

  private final MarshallingStrategy marshallingStrategy;
  private final Type type;

  NingHttpTypedStreamHandler(final long responseMaxSize, final Observer<TypedResponse<T>> stream,
                             final MarshallingStrategy marshallingStrategy, final Type type) {
    super(responseMaxSize, stream);
    this.marshallingStrategy = checkNotNull(marshallingStrategy, "unmarshallingStrategy may not be null");
    this.type = checkNotNull(type, "type may not be null");
  }

  @Override
  TypedResponse<T> supplyResponse(final HttpResponseBodyPart bodyPart, final HttpResponseHeaders headers,
                                  final HttpResponseStatus status) throws IOException {
    final org.asynchttpclient.Response ningResponse = new NettyResponse(status, headers, singletonList(bodyPart));
    final TypedResponse<T> response = new NingResponse<>(ningResponse, type, marshallingStrategy);

    // making sure that we can unmarshall the response
    response.getTypedBody();

    return response;
  }
}