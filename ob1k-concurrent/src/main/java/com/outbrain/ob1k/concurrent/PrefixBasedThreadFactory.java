package com.outbrain.ob1k.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hyadid on 6/17/15.
 */
public class PrefixBasedThreadFactory implements ThreadFactory {

  private final String prefix;
  private final AtomicInteger threadNumber = new AtomicInteger(0);
  private volatile boolean daemonThreads;

  public PrefixBasedThreadFactory(String prefix) {
    this.prefix = prefix;
    this.daemonThreads = false;
  }

  public PrefixBasedThreadFactory withDaemonThreads() {
    this.daemonThreads = true;
    return this;
  }

  @Override
  public Thread newThread(final Runnable r) {
    final Thread t = new Thread(r, prefix + "-pool-thread-" + threadNumber.getAndIncrement());
    t.setDaemon(daemonThreads);
    return t;
  }
}