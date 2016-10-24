package com.outbrain.ob1k.concurrent;

/**
 * Consumes the future computation value
 *
 * @author aronen
 */
@FunctionalInterface
public interface Consumer<T> {

  void consume(Try<T> result);
}