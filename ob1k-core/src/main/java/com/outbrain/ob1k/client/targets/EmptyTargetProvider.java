package com.outbrain.ob1k.client.targets;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author eran 6/21/15.
 */
public class EmptyTargetProvider implements TargetProvider {

  @Override
  public String getTargetLogicalName() {
    return "No Target";
  }

  @Override
  public String provideTarget() {
    throw new NoSuchElementException("No target was set - nothing to provide");
  }

  @Override
  public List<String> provideTargets(final int targetsNum) {
    throw new NoSuchElementException("No target was set - nothing to provide");
  }
}
