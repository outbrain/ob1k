package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.build.BuilderProvider;
import com.outbrain.ob1k.server.build.ConfigureBuilder;
import com.outbrain.ob1k.server.build.ContextPathBuildStep;
import com.outbrain.ob1k.server.build.ExtendableServerBuilder;
import com.outbrain.ob1k.server.build.ResourceMappingBuilder;
import com.outbrain.ob1k.server.build.ServerBuilderState;

public class SpringContextServerBuilder extends ExtendableServerBuilder<SpringContextServerBuilder> {

  private final SpringServiceRegisterBuilder service;
  private final ResourceMappingBuilder resource;
  private final ConfigureBuilder configure;
  private final ServerBuilderState state;

  public static ContextPathBuildStep<SpringContextServerBuilder> newBuilder(final SpringBeanContext ctx) {
    final SpringContextServerBuilder builder = new SpringContextServerBuilder(ctx);
    return new ContextPathBuildStep<>(builder, builder.state);
  }

  private SpringContextServerBuilder(final SpringBeanContext ctx) {
    super();
    state = innerState();
    configure = new ConfigureBuilder(state);
    service = new SpringServiceRegisterBuilder(state, ctx);
    resource = new ResourceMappingBuilder(state);
  }

  public SpringContextServerBuilder configure(final BuilderProvider<ConfigureBuilder> provider) {
    provider.provide(configure);
    return this;
  }

  public SpringContextServerBuilder service(final BuilderProvider<SpringServiceRegisterBuilder> provider) {
    provider.provide(service);
    return this;
  }

  public SpringContextServerBuilder resource(final BuilderProvider<ResourceMappingBuilder> provider) {
    provider.provide(resource);
    return this;
  }

  @Override
  protected SpringContextServerBuilder self() {
    return this;
  }
}
