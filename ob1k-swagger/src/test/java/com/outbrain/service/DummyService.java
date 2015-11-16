package com.outbrain.service;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

public class DummyService implements Service {

  public ComposableFuture<String> echo(final String name) {
    return fromValue("hello " + name);
  }
}
