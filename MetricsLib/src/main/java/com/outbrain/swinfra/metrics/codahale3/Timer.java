package com.outbrain.swinfra.metrics.codahale3;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by aronen on 8/18/14.
 */
public class Timer implements com.outbrain.swinfra.metrics.api.Timer {
  private final com.codahale.metrics.Timer timer;

  private static class Context implements com.outbrain.swinfra.metrics.api.Timer.Context {
    private final com.codahale.metrics.Timer.Context context;

    private Context(com.codahale.metrics.Timer.Context context) {
      this.context = context;
    }

    @Override
    public void stop() {
      this.context.stop();
    }
  }

  public Timer(com.codahale.metrics.Timer timer) {
    this.timer = timer;
  }

  @Override
  public void update(long duration, TimeUnit unit) {
    timer.update(duration, unit);
  }

  @Override
  public <T> T time(Callable<T> event) throws Exception {
    return timer.time(event);
  }

  @Override
  public Context time() {
    return new Context(timer.time());
  }
}
