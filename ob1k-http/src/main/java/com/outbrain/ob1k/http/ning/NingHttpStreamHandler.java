package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.outbrain.ob1k.http.Response;
import io.netty.handler.codec.http.HttpHeaders;
import rx.Observer;
import java.util.Collections;

import org.asynchttpclient.AsyncHandler;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.netty.NettyResponse;

/**
 * @author marenzon
 */
public class NingHttpStreamHandler implements AsyncHandler<Response> {

  private final long responseMaxSize;
  private final Observer<Response> target;
  private volatile HttpHeaders headers;
  private volatile HttpResponseStatus status;
  private volatile long responseSizesAggregated;

  public NingHttpStreamHandler(final long responseMaxSize, final Observer<Response> target) {

    this.responseMaxSize = responseMaxSize;
    this.target = checkNotNull(target, "target may not be null");
  }

  @Override
  public State onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {

    if (responseMaxSize > 0) {
      responseSizesAggregated += bodyPart.length();
      if (responseSizesAggregated > responseMaxSize) {
        onThrowable(new RuntimeException("Response size is bigger than the limit: " + responseMaxSize));
        return State.ABORT;
      }
    }

    final org.asynchttpclient.Response ningResponse = new NettyResponse(status, headers, Collections.singletonList(bodyPart));
    final Response response = new AsyncHttpResponse<>(ningResponse, null, null);

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
  public Response onCompleted() throws Exception {

    target.onCompleted();
    return null;
  }
}
