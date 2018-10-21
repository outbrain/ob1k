package com.outbrain.ob1k.client.targets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.nCopies;

/**
 * A {@link TargetProvider} that provides a fixed target.
 * @author eran 6/21/15.
 */
public class SimpleTargetProvider implements TargetProvider {
  private static final Logger logger = LoggerFactory.getLogger(SimpleTargetProvider.class);
  private final String target;

  public SimpleTargetProvider(final String target) {
    this.target = Objects.requireNonNull(target, "target must not be null");
    logger.info("Target url: {}", target);
  }

  @Override
  public String getTargetLogicalName() {
    return target;
  }

  @Override
  public String provideTarget() {
    return target;
  }

  @Override
  public List<String> provideTargets(final int targetsNum) {
    return nCopies(targetsNum, target);
  }
}
