package com.outbrain.ob1k.server.builder;

public abstract class BuilderStep<S> {

  private final S builder;

  protected BuilderStep(final S builder) {
    this.builder = builder;
  }

  protected S nextStep() {
    return builder;
  }
}
