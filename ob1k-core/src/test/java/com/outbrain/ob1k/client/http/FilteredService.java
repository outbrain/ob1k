package com.outbrain.ob1k.client.http;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by aronen on 10/28/14.
 */
public class FilteredService implements IFilteredService {
  private final AtomicInteger counter = new AtomicInteger();
  private final Random random = new Random();

  public ComposableFuture<String> getNextCode(String name) {
    return ComposableFutures.fromValue(name + "-" + counter.incrementAndGet());
  }

  @Override
  public ComposableFuture<String> getRandomCode(String name) {
    return ComposableFutures.fromValue(name + "-" + random.nextInt(100));
  }
}
