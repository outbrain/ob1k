package com.outbrain.ob1k.server.netty;

import com.ning.http.client.*;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 6/29/14.
 *
 * a client that returns a stream of messages.
 */
public class HttpStreamClient {
  public static void main(final String[] args) {
    final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    final AsyncHttpClient.BoundRequestBuilder requestBuilder = asyncHttpClient.prepareGet("http://localhost:8080/stream?name=asy");
    requestBuilder.addHeader("TE", "chunked, trailers");
    final ListenableFuture<Response> result = requestBuilder.execute(new AsyncHandler<Response>() {
      @Override
      public void onThrowable(final Throwable t) {
        System.out.println("got error");
        t.printStackTrace();
      }

      @Override
      public STATE onBodyPartReceived(final HttpResponseBodyPart bodyPart) throws Exception {
        final String part = new String(bodyPart.getBodyPartBytes());
        System.out.println("got " + (bodyPart.isLast() ? "last " : "") + "body part: " + part);
        return STATE.CONTINUE;
      }

      @Override
      public STATE onStatusReceived(final HttpResponseStatus responseStatus) throws Exception {
        System.out.println("got status: " + responseStatus);
        return STATE.CONTINUE;
      }

      @Override
      public STATE onHeadersReceived(final HttpResponseHeaders headers) throws Exception {
        final boolean hasTrailing = headers.isTraillingHeadersReceived();
        System.out.println("got headers(trailing:" + hasTrailing + "): ");
        final FluentCaseInsensitiveStringsMap headersMap = headers.getHeaders();
        for (final String key : headersMap.keySet()) {
          final String value = headersMap.getFirstValue(key);
          System.out.println(key + ": " + value);
        }

        return STATE.CONTINUE;
      }

      @Override
      public Response onCompleted() throws Exception {
        System.out.println("completed.");
        return null;
      }
    });
    try {
      final Response r = result.get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }
}
