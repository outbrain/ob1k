package com.outbrain.ob1k.consul;

import java.util.List;

public interface LoadBalancer {

  List<String> provideTargets(final List<String> targets, final int targetsNum);

  default void onTargetsChanged(final List<String> newTargets) {}
}
