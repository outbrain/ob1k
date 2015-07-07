package com.outbrain.ob1k.concurrent;

import java.util.concurrent.ScheduledFuture;

/**
 * Created by hyadid on 6/23/15.
 */
public class ScheduledFutureCancellationToken implements CancellationToken {
  private final ScheduledFuture scheduledFuture;
  public ScheduledFutureCancellationToken(final ScheduledFuture scheduledFuture) {
    this.scheduledFuture = scheduledFuture;
  }

  @Override
  public boolean cancel(final boolean mayInterrupt) {
    return scheduledFuture.cancel(mayInterrupt);
  }
}
