package com.outbrain.ob1k.consul.filter;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.outbrain.ob1k.consul.HealthInfoInstance;

import java.util.Collection;
import java.util.Collections;

/**
 * @author marenzon
 */
public class TargetsNotContainingTagsPredicate implements Predicate<HealthInfoInstance> {

  private final Collection<String> tags;

  public static TargetsNotContainingTagsPredicate withoutTag(final String tag) {
    return new TargetsNotContainingTagsPredicate(Collections.singleton(tag));
  }

  public static TargetsNotContainingTagsPredicate withoutTags(final Collection<String> tags) {
    return new TargetsNotContainingTagsPredicate(tags);
  }

  public TargetsNotContainingTagsPredicate(final Collection<String> tags) {
    this.tags = Preconditions.checkNotNull(tags, "tags must not be null");
  }

  @Override
  public boolean apply(final HealthInfoInstance input) {
    return !input.Service.Tags.containsAll(tags);
  }
}