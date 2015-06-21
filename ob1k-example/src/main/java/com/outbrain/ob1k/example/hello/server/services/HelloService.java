package com.outbrain.ob1k.example.hello.server.services;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.example.hello.client.IHelloService;
import rx.Observable;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * @author marenzon
 */
public class HelloService implements IHelloService {

  @Override
  public ComposableFuture<String> helloWorld() {
    return fromValue("Hello World!");
  }

  @Override
  public ComposableFuture<String> helloUser(final String name) {
    return fromValue("Hello, " + name + "!");
  }

  @Override
  public Observable<String> helloUserStream(final String name, final int repeats) {
    return Observable.interval(100, TimeUnit.MILLISECONDS).map(i -> "Hello, " + name + "! (#" + i + ")").take(repeats);
  }
}