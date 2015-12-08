package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.builder.BuilderProvider;
import com.outbrain.ob1k.server.builder.DefaultConfigureBuilder;
import com.outbrain.ob1k.server.builder.DefaultInitialStepBuilder;
import com.outbrain.ob1k.server.builder.DefaultResourceMappingBuilder;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

public class SpringContextServerBuilder extends ExtendableServerBuilder<SpringContextServerBuilder> {

  private final SpringServiceRegisterBuilder service;
  private final DefaultResourceMappingBuilder resource;
  private final DefaultConfigureBuilder configure;
  private final ServerBuilderState state;

  public static DefaultInitialStepBuilder<SpringContextServerBuilder> newBuilder(final SpringBeanContext ctx) {
    final SpringContextServerBuilder builder = new SpringContextServerBuilder(ctx);
    return new DefaultInitialStepBuilder<>(builder, builder.state);
  }

  private SpringContextServerBuilder(final SpringBeanContext ctx) {
    super();
    state = innerState();
    configure = new DefaultConfigureBuilder(state);
    service = new SpringServiceRegisterBuilder(state, ctx);
    resource = new DefaultResourceMappingBuilder(state);
  }

  public SpringContextServerBuilder configure(final BuilderProvider<DefaultConfigureBuilder> provider) {
    provider.provide(configure);
    return this;
  }

  public SpringContextServerBuilder service(final BuilderProvider<SpringServiceRegisterBuilder> provider) {
    provider.provide(service);
    return this;
  }

  public SpringContextServerBuilder resource(final BuilderProvider<DefaultResourceMappingBuilder> provider) {
    provider.provide(resource);
    return this;
  }

  @Override
  protected SpringContextServerBuilder self() {
    return this;
  }
}
