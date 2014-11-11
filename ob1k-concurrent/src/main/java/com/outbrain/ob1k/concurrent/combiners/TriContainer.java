package com.outbrain.ob1k.concurrent.combiners;

/**
 * Created by aronen on 9/2/14.
 */
public class TriContainer<T1, T2, T3> {
  public final T1 first;
  public final T2 second;
  public final T3 third;

  public TriContainer(final T1 first, final T2 second, final T3 third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }
}
