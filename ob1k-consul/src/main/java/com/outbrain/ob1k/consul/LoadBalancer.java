package com.outbrain.ob1k.consul;

import java.util.List;

public interface LoadBalancer {
  List<String> provideTargets(final int targetsNum);

  void onTargetsChanged(final List<String> newTargets);

  default void targetDispatched(String target) {
  }

  default void targetDispatchEnded(String target, boolean success, final long startTimeNanos) {
  }
}
