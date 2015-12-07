package com.outbrain.ob1k.server.builder;

public abstract class BindBuilderSection<E extends ExtendableServerBuilder<E>, S> extends BuilderSection<E> {

  private final S serviceBuilder;

  protected BindBuilderSection(final E builder, final S serviceBuilder) {
    super(builder);
    this.serviceBuilder = serviceBuilder;
  }

  public S service() {
    return serviceBuilder;
  }
}
