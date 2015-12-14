package com.outbrain.ob1k.concurrent;

/**
 * produces a value/error and delivers it to the consumer at some point in the future.
 *
 * @author asy ronen
 */
public interface Producer<T> {
  void produce(Consumer<T> consumer);
}