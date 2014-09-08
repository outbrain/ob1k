package com.outbrain.ob1k.concurrent.handlers;

/**
 * Created by aronen on 8/11/14.
 */
public interface OnErrorHandler {
  void handle(Throwable error);
}
