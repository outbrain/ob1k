package com.outbrain.ob1k.server.builder;

public class DefaultInitialStepBuilder<E extends ExtendableServerBuilder<E>> extends BuilderStep<E> {

  private final ServerBuilderState state;

  public DefaultInitialStepBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
  }

  public E contextPath(final String contextPath) {
    return contextPath(contextPath, extractAppName(contextPath));
  }

  public E contextPath(final String contextPath, final String appName) {
    state.setContextPath(contextPath);
    state.setAppName(appName);
    return nextStep();
  }

  private static String extractAppName(final String contextPath) {
    if (contextPath.startsWith("/"))
      return contextPath.substring(1);

    return contextPath;
  }
}
