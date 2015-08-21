package com.outbrain.ob1k.example.hello.api;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import rx.Observable;

/**
 * @author marenzon
 */
public interface HelloService extends Service {

  ComposableFuture<String> helloWorld();

  ComposableFuture<String> helloUser(String name);

  Observable<String> helloUserStream(String name, int repeats);
}