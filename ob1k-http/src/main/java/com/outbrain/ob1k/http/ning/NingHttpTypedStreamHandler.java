package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singletonList;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.providers.netty.response.NettyResponse;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import rx.Observer;

import java.lang.reflect.Type;

/**
 * @author aronen, marenzon
 * @param <T>
 */
public class NingHttpTypedStreamHandler<T> implements AsyncHandler<T> {

  private final long responseMaxSize;
  private final Observer<TypedResponse<T>> target;
  private final MarshallingStrategy marshallingStrategy;
  private final Type type;
  private volatile HttpResponseHeaders headers;
  private volatile HttpResponseStatus status;
  private volatile long responseSizesAggregated;

  public NingHttpTypedStreamHandler(final long responseMaxSize, final Observer<TypedResponse<T>> target,
                                    final MarshallingStrategy marshallingStrategy, final Type type) {

    this.responseMaxSize = responseMaxSize;
    this.target = checkNotNull(target, "target may not be null");
    this.marshallingStrategy = checkNotNull(marshallingStrategy, "unmarshallingStrategy may not be null");
    this.type = checkNotNull(type, "type may not be null");
  }

  @Override
  public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {

    if (responseMaxSize > 0) {
      responseSizesAggregated += bodyPart.length();
      if (responseSizesAggregated > responseMaxSize) {
        onThrowable(new RuntimeException("Response size is bigger than the limit: " + responseMaxSize));
        return STATE.ABORT;
      }
    }

    final com.ning.http.client.Response ningResponse = new NettyResponse(status, headers, singletonList(bodyPart));
    final TypedResponse<T> response = new NingResponse<>(ningResponse, type, marshallingStrategy);

    try {
      // making sure that we can unmarshall the response
      response.getTypedBody();
    } catch (final Exception e) {
      // if the unmarshall failed, no reason to continuing the stream
      onThrowable(e);
      return STATE.ABORT;
    }

    target.onNext(response);
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