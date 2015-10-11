package com.outbrain.ob1k.consul.filter;

import com.google.common.base.Predicate;
import com.outbrain.ob1k.consul.HealthInfoInstance;

/**
 * A targets filter that returns true for all targets
 * @author Eran Harel
 */
public class AllTargetsPredicate implements Predicate<HealthInfoInstance> {

  public static final AllTargetsPredicate INSTANCE = new AllTargetsPredicate();

  private AllTargetsPredicate() {}

  @Override
  public boolean apply(final HealthInfoInstance input) {
    return true;
  }
}
