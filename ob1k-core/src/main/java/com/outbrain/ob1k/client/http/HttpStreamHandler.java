package com.outbrain.ob1k.client.http;

import com.ning.http.client.*;
import com.ning.http.client.providers.netty.NettyResponse;
import com.ning.http.client.providers.netty.ResponseBodyPart;
import com.outbrain.ob1k.common.marshalling.ChunkHeader;
import rx.Observer;

import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * Created by aronen on 6/30/14.
 *
 * a generator for hot Observable that emits events as they come from the http client.
 */
public class HttpStreamHandler<T> implements AsyncHandler<T> {
  private final RequestBuilder builder;
  private final Type respType;
  private final Observer<T> target;

  private volatile HttpResponseHeaders headers;
  private volatile HttpResponseStatus status;

  public HttpStreamHandler(final Observer<T> target, final RequestBuilder builder, final Type respType) {
    this.builder = builder;
    this.respType = respType;
    this.target = target;
  }

  @Override
  public void onThrowable(final Throwable error) {
    target.onError(error);
  }

  @Override
  public T onCompleted() throws Exception {
    target.onCompleted();
    return null;
  }

  @Override
  public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
    final Response response = new NettyResponse(status, headers, Arrays.asList(bodyPart));
    final String body = response.getResponseBody();

    if (body.startsWith(ChunkHeader.ELEMENT_HEADER)) {
      //advance the read index beyond the chunk header.
      ((ResponseBodyPart)bodyPart).getChannelBuffer().readerIndex(ChunkHeader.ELEMENT_HEADER.length());

      @SuppressWarnings("unchecked")
      final T element = (T) builder.unmarshallResponse(response, respType);
      target.onNext(element);

    } else if (body.startsWith(ChunkHeader.ERROR_HEADER)) {
      //advance the read index beyond the chunk header.
      ((ResponseBodyPart)bodyPart).getChannelBuffer().readerIndex(ChunkHeader.ERROR_HEADER.length());

      final Exception error = new RuntimeException(response.getResponseBody());
      onThrowable(error);
      // should we return STATE.ABORT ?
    } else {
      // raw stream mode.
      @SuppressWarnings("unchecked")
      final T element = (T) builder.unmarshallResponse(response, respType);
      target.onNext(element);
    }

    return STATE.CONTINUE;
  }

  @Override
  public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
    this.status = responseStatus;
    return STATE.CONTINUE;
  }

  @Override
  public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
    this.headers = headers;
    return STATE.CONTINUE;
  }

}
