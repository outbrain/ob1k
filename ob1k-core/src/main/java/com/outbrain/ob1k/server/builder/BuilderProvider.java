package com.outbrain.ob1k.server.builder;

public interface BuilderProvider<T> {

  void provide(final T builder);
}
