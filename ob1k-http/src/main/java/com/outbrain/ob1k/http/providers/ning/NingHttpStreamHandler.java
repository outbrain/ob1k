package com.outbrain.ob1k.http.providers.ning;

import com.outbrain.ob1k.http.Response;
import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import rx.Observer;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author marenzon
 */
abstract class NingHttpStreamHandler<T extends Response> implements AsyncHandler<T> {

  private final long responseMaxSize;
  private final Observer<T> stream;

  private volatile HttpResponseHeaders headers;
  private volatile HttpResponseStatus status;
  private volatile long responseSizesAggregated;

  NingHttpStreamHandler(final long responseMaxSize, final Observer<T> stream) {
    this.responseMaxSize = responseMaxSize;
    this.stream = checkNotNull(stream, "stream may not be null");
  }

  @Override
  public State onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
    // empty chunk, identifying end of stream
    if (bodyPart.isLast()) {
      return State.CONTINUE;
    }

    if (responseMaxSize > 0) {
      responseSizesAggregated += bodyPart.length();
      if (responseSizesAggregated > responseMaxSize) {
        stream.onError(new RuntimeException("Response size is bigger than the limit: " + responseMaxSize));
        return State.ABORT;
      }
    }

    try {
      final T response = supplyResponse(bodyPart, headers, status);
      stream.onNext(response);
    } catch (final Exception e) {
      stream.onError(e);
      return State.ABORT;
    }

    return State.CONTINUE;
  }

  @Override
  public State onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
    this.status = responseStatus;
    return State.CONTINUE;
  }

  @Override
  public State onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
    this.headers = headers;
    return State.CONTINUE;
  }

  @Override
  public void onThrowable(final Throwable error) {
    stream.onError(error);
  }

  @Override
  public T onCompleted() throws Exception {
    stream.onCompleted();
    return null;
  }

  abstract T supplyResponse(HttpResponseBodyPart bodyPart, HttpResponseHeaders headers,
                            HttpResponseStatus status) throws IOException;
}