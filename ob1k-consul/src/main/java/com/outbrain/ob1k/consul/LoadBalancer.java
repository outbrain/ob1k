package com.outbrain.ob1k.consul;

import java.util.List;

public interface LoadBalancer {
  List<String> provideTargets(final int targetsNum);

  void onTargetsChanged(final List<String> newTargets);
}
