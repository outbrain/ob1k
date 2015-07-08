package com.outbrain.ob1k.security.common;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;

/**
 * Created by gmarom on 6/24/15
 */
public class SecureServiceImpl implements SecureService {
  @Override
  public ComposableFuture<String> returnString(final String val) {
    return ComposableFutures.fromValue(val);
  }

  @Override
  public String returnStringSync(final String val) {
    return val;
  }
}
