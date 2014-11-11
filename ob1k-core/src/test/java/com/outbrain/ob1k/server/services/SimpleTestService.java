package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.entities.TestEntity;

/**
 * Created by aronen on 10/6/14.
 */
public interface SimpleTestService extends Service {
  ComposableFuture<String> method1(int val1, String val2, TestEntity entity);
  ComposableFuture<TestEntity> method2(int val1, String val2);

  ComposableFuture<Boolean> slowMethod(long delayTimeMs);

  ComposableFuture<Integer> nextRandom();
}
