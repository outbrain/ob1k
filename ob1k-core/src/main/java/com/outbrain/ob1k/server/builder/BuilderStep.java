package com.outbrain.ob1k.server.builder;

/**
 * An abstract class that helps defines a Builder Step.
 * It holds the next step that must be returned as the return value of the step method.
 *
 * @param <NextStep> the next step - to be used as the return value for the step method.
 */
public abstract class BuilderStep<NextStep> {

  private final NextStep nextStep;

  protected BuilderStep(final NextStep nextStep) {
    this.nextStep = nextStep;
  }

  protected NextStep nextStep() {
    return nextStep;
  }
}
