package com.outbrain.ob1k.server.builder;

/**
 * Required step in every ob1k server builder to define the context path.
 *
 * @param <Next> nextStep
 */
public class ContextPathBuildStep<Next> extends BuilderStep<Next> {

  private final ServerBuilderState state;

  public ContextPathBuildStep(final Next nextStep, final ServerBuilderState state) {
    super(nextStep);
    this.state = state;
  }

  public Next contextPath(final String contextPath) {
    return contextPath(contextPath, extractAppName(contextPath));
  }

  public Next contextPath(final String contextPath, final String appName) {
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
