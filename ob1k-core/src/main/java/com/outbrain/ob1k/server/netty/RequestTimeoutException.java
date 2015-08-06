package com.outbrain.ob1k.server.netty;

import java.util.concurrent.TimeoutException;

/**
 * Indicates that request timeout occurred in the server
 *
 * @author marenzon
 */
public class RequestTimeoutException extends TimeoutException {

  public RequestTimeoutException(final String message) {
    super(message);
  }
}