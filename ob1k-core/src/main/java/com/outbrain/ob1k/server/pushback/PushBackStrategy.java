package com.outbrain.ob1k.server.pushback;

/**
 * Strategy to determine whether to push back requests using PushBackFilter
 */
public interface PushBackStrategy {

  /**
   * Logic to determine whether current request should be pushed back
   *
   * @return  true iff request should be allowed through
   */
  boolean allowRequest();

  /**
   * Guaranteed to be called when request handling is done
   *
   * @param allowedRequest  was this request pushed back or not
   */
  void done(boolean allowedRequest);

  /**
   * @return  exception to be thrown if request should be pushed back
   */
  PushBackException generateExceptionOnPushBack();
}
