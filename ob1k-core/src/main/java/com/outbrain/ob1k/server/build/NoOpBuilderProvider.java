package com.outbrain.ob1k.server.build;

public class NoOpBuilderProvider<T> implements BuilderProvider<T> {

  public static <T> NoOpBuilderProvider<T> getInstance() {
    return new NoOpBuilderProvider<>();
  }

  private NoOpBuilderProvider() {
  }

  @Override
  public void provide(final T buildSection) {
    // do nothing
  }
}
