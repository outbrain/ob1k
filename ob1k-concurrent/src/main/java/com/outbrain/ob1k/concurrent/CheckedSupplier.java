package com.outbrain.ob1k.concurrent;

/**
 * @author marenzo
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

  /**
   * Gets a result.
   *
   * @return a result
   */
  T get() throws Exception;
}