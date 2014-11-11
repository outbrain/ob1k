package com.outbrain.ob1k.server;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

/**
 * Created by aronen on 6/9/14.
 *
 * an interface for sending results back to the client.
 * result can be either single or a part of a stream.
 */
public interface ResponseHandler {
  void handleAsyncResponse(ComposableFuture<Object> response);
  void handleStreamResponse(Observable<Object> response, boolean rawStream);
}
