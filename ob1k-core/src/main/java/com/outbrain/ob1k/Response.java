package com.outbrain.ob1k;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * User: aronen
 * Date: 6/30/13
 * Time: 2:44 PM
 */
public interface Response {

  HttpResponseStatus getStatus();

  String getRawContent();
}
