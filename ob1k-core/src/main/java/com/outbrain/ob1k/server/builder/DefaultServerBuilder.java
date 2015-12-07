package com.outbrain.ob1k.server.builder;

public class DefaultServerBuilder extends ExtendableServerBuilder<DefaultServerBuilder> {

  private final DefaultServiceRegisterBuilder<DefaultServerBuilder> service;
  private final DefaultResourceMappingBuilder<DefaultServerBuilder> resource;
  private final DefaultConfigureBuilder<DefaultServerBuilder> configure;
  private final ServerBuilderState state;

  public static DefaultInitialStepBuilder<DefaultServerBuilder> newBuilder() {
    final DefaultServerBuilder builder = new DefaultServerBuilder();
    return new DefaultInitialStepBuilder<>(builder, builder.state);
  }

  private DefaultServerBuilder() {
    super();
    state = innerState();
    configure = new DefaultConfigureBuilder<>(this, state);
    service = new DefaultServiceRegisterBuilder<>(this, state);
    resource = new DefaultResourceMappingBuilder<>(this, state);
  }

  public DefaultConfigureBuilder<DefaultServerBuilder> configure() {
    return configure;
  }

  public DefaultResourceMappingBuilder<DefaultServerBuilder> resource() {
    return resource;
  }

  public DefaultServiceRegisterBuilder<DefaultServerBuilder> service() {
    return service;
  }

  @Override
  protected DefaultServerBuilder self() {
    return this;
  }


}
