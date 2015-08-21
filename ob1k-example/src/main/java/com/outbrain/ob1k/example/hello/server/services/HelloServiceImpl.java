package com.outbrain.ob1k.example.hello.server.services;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.example.hello.api.HelloService;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static rx.Observable.interval;

/**
 * Hello Service Implementation
 * Main service of our hello server example
 *
 * @author marenzon
 */
public class HelloServiceImpl implements HelloService {

  /**
   * Creates new future with our response message
   *
   * @return future of result
   */
  @Override
  public ComposableFuture<String> helloWorld() {
    return fromValue("Hello World!");
  }

  /**
   * Endpoint which receives username as input and returns future
   * with the message
   *
   * @param name username (comes either from request body or as query param)
   * @return future of result
   */
  @Override
  public ComposableFuture<String> helloUser(final String name) {
    return fromValue("Hello, " + name + "!");
  }

  /**
   * Endpoint which receives username and repeats number,
   * and returns stream of messages limited by the repeat number size
   *
   * @param name username (comes either from request body or as query param)
   * @param repeats repeats (comes either from request body or as query param)
   * @return stream of results (>= repeats)
   */
  @Override
  public Observable<String> helloUserStream(final String name, final int repeats) {
    return interval(100, TimeUnit.MILLISECONDS).map(i -> "Hello, " + name + "! (#" + i + ")").take(repeats);
  }
}