package com.outbrain.ob1k.server.build;

public interface BuilderProvider<T> {

  void provide(final T builder);
}
