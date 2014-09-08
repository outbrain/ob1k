package com.outbrain.ob1k.concurrent.combiners;

/**
 * Created by aronen on 9/2/14.
 */
public class BiContainer<T1, T2> {
  public final T1 left;
  public final T2 right;

  public BiContainer(final T1 left, final T2 right) {
    this.left = left;
    this.right = right;
  }
}
