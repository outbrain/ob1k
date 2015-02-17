package com.outbrain.ob1k.concurrent;

import com.outbrain.ob1k.concurrent.Consumer;

/**
 * produces a value/error and delivers it to the consumer at some point in the future.
 *
 * @author asy ronen
 */
public interface Producer<T> {
  public void produce(Consumer<T> consumer);
}