package com.outbrain.ob1k.concurrent;

import java.util.List;

/**
 * User: aronen
 * Date: 6/10/13
 * Time: 5:07 PM
 */
public class AggregatedException extends Exception {
  private List<Throwable> internals;

  public AggregatedException(final List<Throwable> internals) {
    super(createAggregatedMessage(internals));
    this.internals = internals;
  }

  private static String createAggregatedMessage(final List<Throwable> internals) {
    final StringBuilder message = new StringBuilder("compound errors:\n");
    for(final Throwable e : internals) {
      message.append(e.getMessage()).append("\n");
    }

    return message.toString();
  }

  public List<Throwable> getCauses() {
    return internals;
  }
}
