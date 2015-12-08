package com.outbrain.ob1k.server.build;

public class ServerBuilder extends ExtendableServerBuilder<ServerBuilder> {

  private final ServiceRegisterBuilder service;
  private final ResourceMappingBuilder resource;
  private final ConfigureBuilder configure;
  private final ServerBuilderState state;

  public static ContextPathBuildStep<ServerBuilder> newBuilder() {
    final ServerBuilder builder = new ServerBuilder();
    return new ContextPathBuildStep<>(builder, builder.state);
  }

  private ServerBuilder() {
    super();
    state = innerState();
    configure = new ConfigureBuilder(state);
    service = new ServiceRegisterBuilder(state);
    resource = new ResourceMappingBuilder(state);
  }

  public ServerBuilder configure(final BuilderProvider<ConfigureBuilder> provider) {
    provider.provide(configure);
    return this;
  }

  public ServerBuilder service(final BuilderProvider<ServiceRegisterBuilder> provider) {
    provider.provide(service);
    return this;
  }

  public ServerBuilder resource(final BuilderProvider<ResourceMappingBuilder> provider) {
    provider.provide(resource);
    return this;
  }

  @Override
  protected ServerBuilder self() {
    return this;
  }
}
