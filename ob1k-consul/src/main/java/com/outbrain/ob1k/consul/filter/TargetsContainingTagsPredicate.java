package com.outbrain.ob1k.consul.filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.outbrain.ob1k.consul.HealthInfoInstance;

import java.util.Collection;

/**
 * A targets filter that returns true iff the target contains all the tags specified in the constructor.
 * @author Eran Harel
 */
public class TargetsContainingTagsPredicate implements Predicate<HealthInfoInstance> {

  private final Collection<String> tags;

  public TargetsContainingTagsPredicate(final Collection<String> tags) {
    this.tags = Preconditions.checkNotNull(tags, "tags must not be null");
  }

  @Override
  public boolean apply(final HealthInfoInstance input) {
    return input.Service.Tags.containsAll(tags);
  }
}
