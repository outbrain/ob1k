package com.outbrain.ob1k.concurrent;

/**
 * User: aronen
 * Date: 6/13/13
 * Time: 9:40 PM
 */
public final class ComposableFutureConfig {
  private ComposableFutureConfig() {}

  public static volatile int corePoolSize = 50;
  public static volatile int maxOPoolSize = 100;
  public static volatile int timerSize = 10;
}
