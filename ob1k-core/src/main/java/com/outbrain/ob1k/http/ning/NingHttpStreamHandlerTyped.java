package com.outbrain.ob1k.http.ning;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.providers.netty.response.NettyResponse;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.marshalling.UnmarshallingStrategy;
import rx.Observer;

import java.lang.reflect.Type;
import java.util.Collections;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author aronen, marenzon
 * @param <T>
 */
public class NingHttpStreamHandlerTyped<T> implements AsyncHandler<T> {

  private final Observer<TypedResponse<T>> target;
  private final UnmarshallingStrategy unmarshallingStrategy;
  private final Type type;

  private volatile HttpResponseHeaders headers;
  private volatile HttpResponseStatus status;

  public NingHttpStreamHandlerTyped(final Observer<TypedResponse<T>> target, final UnmarshallingStrategy unmarshallingStrategy, final Type type) {

    this.target = checkNotNull(target, "target may not be null");
    this.unmarshallingStrategy = checkNotNull(unmarshallingStrategy, "unmarshallingStrategy may not be null");
    this.type = checkNotNull(type, "type may not be null");
  }

  @Override
  public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {

    final com.ning.http.client.Response ningResponse = new NettyResponse(status, headers, Collections.singletonList(bodyPart));
    final TypedResponse<T> response = new NingResponse<>(ningResponse, type, unmarshallingStrategy);

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