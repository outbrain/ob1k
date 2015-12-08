package com.outbrain.ob1k.server.builder;

public abstract class BuilderStep<E> {

  private final E builder;

  protected BuilderStep(final E builder) {
    this.builder = builder;
  }

  protected E nextStep() {
    return builder;
  }
}
