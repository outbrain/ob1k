package com.outbrain.ob1k.http.ning;

import com.outbrain.ob1k.http.Response;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.netty.NettyResponse;
import rx.Observer;

import static java.util.Collections.singletonList;

/**
 * @author marenzon
 */
class NingHttpResponseStreamHandler extends NingHttpStreamHandler<Response> {

  public NingHttpResponseStreamHandler(final long responseMaxSize, final Observer<Response> stream) {
    super(responseMaxSize, stream);
  }

  @Override
  public Response supplyResponse(final HttpResponseBodyPart bodyPart) {
    final org.asynchttpclient.Response ningResponse = new NettyResponse(status, headers, singletonList(bodyPart));
    return new NingResponse<>(ningResponse, null, null);
  }
}