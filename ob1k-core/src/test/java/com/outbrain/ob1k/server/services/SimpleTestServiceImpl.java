package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.server.entities.OtherEntity;
import com.outbrain.ob1k.server.entities.TestEntity;
import com.outbrain.ob1k.server.entities.TestEnum;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;
import static com.outbrain.ob1k.concurrent.ComposableFutures.schedule;

/**
 * Created by aronen on 10/6/14.
 */
public class SimpleTestServiceImpl implements SimpleTestService {
  @Override
  public ComposableFuture<String> method1(int val1, String val2, TestEntity entity) {
    return fromValue("res: " + val1 + ", " + val2 + ", " + entity.getName());
  }

  @Override
  public ComposableFuture<TestEntity> method2(int val1, String val2) {
    Set<Long> ids = new HashSet<>(Arrays.asList((long)val1));
    OtherEntity other1 = new OtherEntity(1, "2");
    return fromValue(new TestEntity(ids, val2, new TestEnum[]{TestEnum.Value1, TestEnum.Value1}, Arrays.asList(other1)));
  }

  @Override
  public ComposableFuture<Boolean> slowMethod(long delayTimeMs) {
    return schedule(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return true;
      }
    }, delayTimeMs, TimeUnit.MILLISECONDS);
  }
}
