package com.outbrain.ob1k.client.http;

import com.outbrain.ob1k.Response;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.Service;
import rx.Observable;

import java.util.List;

/**
 * User: aronen
 * Date: 6/23/13
 * Time: 3:08 PM
 */
public interface IHelloService extends Service {
  ComposableFuture<String> hello(String name);
  ComposableFuture<String> helloWorld();
  ComposableFuture<Integer> getRandomNumber();
  ComposableFuture<String> helloFilter(String name);
  ComposableFuture<Response> emptyString();
  ComposableFuture<List<TestBean>> increaseAge(List<TestBean> beans, String newHabit);
  ComposableFuture<Boolean> sleep(int milliseconds);
  Observable<String> getMessages(String name, int iterations, boolean failAtEnd);
}
