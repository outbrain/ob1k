package com.outbrain.ob1k.concurrent;

/**
 * Produces a computation value/error, and provides it to
 * the consumer at some point of execution time.
 *
 * @author aronen
 */
@FunctionalInterface
public interface Producer<T> {

  void produce(Consumer<T> consumer);
}