package com.outbrain.ob1k.server.builder;

public abstract class ServiceBuilderSection<E extends ExtendableServerBuilder<E>, S extends ServiceBuilderSection<E, S>> extends BuilderSection<E> {

  protected ServiceBuilderSection(final E builder) {
    super(builder);
  }

  public S service() {
    return self();
  }

  protected abstract S self();
}
