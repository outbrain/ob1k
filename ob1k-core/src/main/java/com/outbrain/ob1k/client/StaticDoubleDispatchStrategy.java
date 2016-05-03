package com.outbrain.ob1k.client;

import com.outbrain.ob1k.concurrent.Try;

/**
 * A simple double dispatch strategy with static threshold
 *
 * @author lifey
 */
public class StaticDoubleDispatchStrategy  implements DoubleDispatchStrategy {
  private final long duration;

  public StaticDoubleDispatchStrategy(final long durationMs) {
    this.duration = durationMs;
  }

  @Override
  public long getDoubleDispatchIntervalMs() {
    return duration;
  }

  @Override
  public void onComplete(Try result, long startTimeMs) {
    // do nothing we are static
  }
}
