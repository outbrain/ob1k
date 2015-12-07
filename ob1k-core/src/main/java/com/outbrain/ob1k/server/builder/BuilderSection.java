package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.server.Server;

public abstract class BuilderSection<E extends ExtendableServerBuilder<E>> {

  private final E builder;

  protected BuilderSection(E builder) {
    this.builder = builder;
  }

  public E and() {
    return builder;
  }

  public Server build() {
    return builder.build();
  }
}
