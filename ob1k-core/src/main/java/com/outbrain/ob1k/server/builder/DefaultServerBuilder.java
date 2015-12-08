package com.outbrain.ob1k.server.builder;

public class DefaultServerBuilder extends ExtendableServerBuilder<DefaultServerBuilder> {

  private final DefaultServiceRegisterBuilder service;
  private final DefaultResourceMappingBuilder resource;
  private final DefaultConfigureBuilder configure;
  private final ServerBuilderState state;

  public static DefaultInitialStepBuilder<DefaultServerBuilder> newBuilder() {
    final DefaultServerBuilder builder = new DefaultServerBuilder();
    return new DefaultInitialStepBuilder<>(builder, builder.state);
  }

  private DefaultServerBuilder() {
    super();
    state = innerState();
    configure = new DefaultConfigureBuilder(state);
    service = new DefaultServiceRegisterBuilder(state);
    resource = new DefaultResourceMappingBuilder(state);
  }

  public DefaultServerBuilder configure(final BuilderProvider<DefaultConfigureBuilder> provider) {
    provider.provide(configure);
    return this;
  }

  public DefaultServerBuilder service(final BuilderProvider<DefaultServiceRegisterBuilder> provider) {
    provider.provide(service);
    return this;
  }

  public DefaultServerBuilder resource(final BuilderProvider<DefaultResourceMappingBuilder> provider) {
    provider.provide(resource);
    return this;
  }

  @Override
  protected DefaultServerBuilder self() {
    return this;
  }
}
