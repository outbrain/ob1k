package com.outbrain.ob1k.security.common;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by gmarom on 6/24/15
 */
public interface UnsecureService extends Service {
  ComposableFuture<String> returnString(String val);
}
