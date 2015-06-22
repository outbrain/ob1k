package com.outbrain.ob1k.example.hello.client;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

/**
 * @author marenzon
 */
public interface IHelloService extends Service {

  ComposableFuture<String> helloWorld();

  ComposableFuture<String> helloUser(String name);

  Observable<String> helloUserStream(String name, int repeats);
}