package com.outbrain.ob1k.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * User: aronen
 * Date: 10/2/13
 * Time: 12:51 AM
 */
public class ScheduledSimpleComposableFuture<T> extends SimpleComposableFuture<T>
    implements RunnableScheduledFuture<T>, ScheduledComposableFuture<T> {

  private final long time;
  public ScheduledSimpleComposableFuture(final Callable<T> task, final long time) {
    super(task);
    this.time = time;
  }

  @Override
  public boolean isPeriodic() {
    return false;
  }

  public long getDelay(final TimeUnit unit) {
    return unit.convert(time - System.nanoTime(), TimeUnit.NANOSECONDS);
  }

  @Override
  public int compareTo(final Delayed other) {
    if (other == this) // compare zero ONLY if same object
      return 0;

    if (other instanceof ScheduledSimpleComposableFuture) {
      final ScheduledSimpleComposableFuture<?> x = (ScheduledSimpleComposableFuture<?>) other;
      final long diff = time - x.time;
      if (diff < 0)
        return -1;
      else if (diff > 0)
        return 1;
      else
        return 0;
    }

    final long d = (getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS));
    return (d == 0) ? 0 : ((d < 0) ? -1 : 1);
  }
}
