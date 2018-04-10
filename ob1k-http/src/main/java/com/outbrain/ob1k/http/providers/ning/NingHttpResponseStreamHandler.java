package com.outbrain.ob1k.http.providers.ning;

import com.outbrain.ob1k.http.Response;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseHeaders;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.netty.NettyResponse;
import rx.Observer;

import java.io.IOException;

import static java.util.Collections.singletonList;

/**
 * @author marenzon
 */
class NingHttpResponseStreamHandler extends NingHttpStreamHandler<Response> {

  NingHttpResponseStreamHandler(final long responseMaxSize, final Observer<Response> stream) {
    super(responseMaxSize, stream);
  }

  @Override
  Response supplyResponse(final HttpResponseBodyPart bodyPart, final HttpResponseHeaders headers,
                          final HttpResponseStatus status) throws IOException {
    final org.asynchttpclient.Response ningResponse = new NettyResponse(status, headers, singletonList(bodyPart));
    return new NingResponse<>(ningResponse, null, null);
  }
}