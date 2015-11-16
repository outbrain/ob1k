package com.outbrain.ob1k.swagger.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

public class Ob1kDummyService implements Service {

    public ComposableFuture<String> ignored(final String name) {
      return fromValue("ignored " + name);
    }

}
