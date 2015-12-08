package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.builder.ContextPathBuildStep;
import com.outbrain.ob1k.server.builder.ServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.spring.SpringServiceRegisterBuilder.SpringServiceRegisterBuilderSection;

/**
 * A ServerBuilder that defines an API of one additional section for registering services through spring context.
 *
 *
 */
public class SpringContextServerBuilder extends ServerBuilder<SpringContextServerBuilder> {

  private final SpringServiceRegisterBuilder service;
  private final ServerBuilderState state;

  public static ContextPathBuildStep<SpringContextServerBuilder> newBuilder(final SpringBeanContext ctx) {
    final SpringContextServerBuilder builder = new SpringContextServerBuilder(ctx);
    return new ContextPathBuildStep<>(builder, builder.state);
  }

  private SpringContextServerBuilder(final SpringBeanContext ctx) {
    super();
    state = innerState();
    service = new SpringServiceRegisterBuilder(state, ctx);
  }

  public SpringContextServerBuilder serviceFromContext(final SpringServiceRegisterBuilderSection section) {
    section.apply(service);
    return self();
  }
}
