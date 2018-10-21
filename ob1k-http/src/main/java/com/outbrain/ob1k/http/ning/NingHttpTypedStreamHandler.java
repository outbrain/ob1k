package com.outbrain.ob1k.http.ning;


import static java.util.Collections.singletonList;

import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import io.netty.handler.codec.http.HttpHeaders;
import rx.Observer;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.netty.NettyResponse;

/**
 * @author aronen, marenzon
 * @param <T>
 */
public class NingHttpTypedStreamHandler<T> implements AsyncHandler<T> {

  private final long responseMaxSize;
  private final Observer<TypedResponse<T>> target;
  private final MarshallingStrategy marshallingStrategy;
  private final Type type;
  private volatile HttpHeaders headers;
  private volatile HttpResponseStatus status;
  private AtomicLong responseSizesAggregated;

  public NingHttpTypedStreamHandler(final long responseMaxSize, final Observer<TypedResponse<T>> target,
                                    final MarshallingStrategy marshallingStrategy, final Type type) {

    this.responseMaxSize = responseMaxSize;
    this.target = Objects.requireNonNull(target, "target may not be null");
    this.marshallingStrategy = Objects.requireNonNull(marshallingStrategy, "unmarshallingStrategy may not be null");
    this.type = Objects.requireNonNull(type, "type may not be null");
  }

  @Override
  public State onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
    if (responseMaxSize > 0) {
      if (responseSizesAggregated.addAndGet(bodyPart.length()) > responseMaxSize) {
        onThrowable(new RuntimeException("Response size is bigger than the limit: " + responseMaxSize));
        return State.ABORT;
      }
    }

    final org.asynchttpclient.Response ningResponse = new NettyResponse(status, headers, singletonList(bodyPart));
    final TypedResponse<T> response = new AsyncHttpResponse<>(ningResponse, type, marshallingStrategy);

    if (bodyPart.isLast()) {
      return State.CONTINUE;
    }
    try {
      // making sure that we can unmarshall the response
      response.getTypedBody();
    } catch (final Exception e) {
      // if the unmarshall failed, no reason to continuing the stream
      onThrowable(e);
      return State.ABORT;
    }

    target.onNext(response);
    return State.CONTINUE;
  }

  @Override
  public State onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {

    this.status = responseStatus;
    return State.CONTINUE;
  }

  @Override
  public State onHeadersReceived(final HttpHeaders headers) throws Exception {

    this.headers = headers;
    return State.CONTINUE;
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
}
