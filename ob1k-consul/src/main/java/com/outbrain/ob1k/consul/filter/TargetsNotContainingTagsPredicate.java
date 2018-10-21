package com.outbrain.ob1k.consul.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.google.common.base.Predicate;
import com.outbrain.ob1k.consul.HealthInfoInstance;

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
    this.tags = Objects.requireNonNull(tags, "tags must not be null");
  }

  @Override
  public boolean apply(final HealthInfoInstance input) {
    return !input.Service.Tags.containsAll(tags);
  }
}
