package com.outbrain.ob1k.concurrent;

/**
 * consumes the futures value.
 * @author asy ronen
 */
public interface Consumer<T> {
  public void consume(Try<T> result);
}