package com.outbrain.ob1k.http.ning;

import static com.google.common.base.Preconditions.checkNotNull;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.providers.netty.response.NettyResponse;
import com.outbrain.ob1k.http.Response;
import rx.Observer;
import java.util.Collections;

/**
 * @author marenzon
 */
public class NingHttpStreamHandler implements AsyncHandler<Response> {

  private final Observer<Response> target;

  private volatile HttpResponseHeaders headers;
  private volatile HttpResponseStatus status;

  public NingHttpStreamHandler(final Observer<Response> target) {

    this.target = checkNotNull(target, "target may not be null");
  }

  @Override
  public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {

    final com.ning.http.client.Response ningResponse = new NettyResponse(status, headers, Collections.singletonList(bodyPart));
    final Response response = new NingResponse<>(ningResponse, null, null);

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
  public Response onCompleted() throws Exception {

    target.onCompleted();
    return null;
  }
}