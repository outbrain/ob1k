package com.outbrain.ob1k.client.http;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

/**
 * Created by aronen on 10/28/14.
 */
public interface IFilteredService extends Service {
  ComposableFuture<String> getNextCode(String name);
  ComposableFuture<String> getRandomCode(String name);
}
