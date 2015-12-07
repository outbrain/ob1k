package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.server.builder.DefaultConfigureBuilder;
import com.outbrain.ob1k.server.builder.DefaultInitialStepBuilder;
import com.outbrain.ob1k.server.builder.DefaultResourceMappingBuilder;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

public class SpringContextServerBuilder extends ExtendableServerBuilder<SpringContextServerBuilder> {

  private final SpringServiceRegisterBuilder<SpringContextServerBuilder> service;
  private final DefaultResourceMappingBuilder<SpringContextServerBuilder> resource;
  private final DefaultConfigureBuilder<SpringContextServerBuilder> configure;
  private final ServerBuilderState state;

  public static DefaultInitialStepBuilder<SpringContextServerBuilder> newBuilder(final SpringBeanContext ctx) {
    final SpringContextServerBuilder builder = new SpringContextServerBuilder(ctx);
    return new DefaultInitialStepBuilder<>(builder, builder.state);
  }

  private SpringContextServerBuilder(final SpringBeanContext ctx) {
    super();
    state = innerState();
    configure = new DefaultConfigureBuilder<>(this, state);
    service = new SpringServiceRegisterBuilder<>(this, state, ctx);
    resource = new DefaultResourceMappingBuilder<>(this, state);
  }

  public DefaultConfigureBuilder<SpringContextServerBuilder> configure() {
    return configure;
  }

  public DefaultResourceMappingBuilder<SpringContextServerBuilder> resource() {
    return resource;
  }

  public SpringServiceRegisterBuilder<SpringContextServerBuilder> service() {
    return service;
  }

  @Override
  protected SpringContextServerBuilder self() {
    return this;
  }


}
