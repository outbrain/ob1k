package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.server.builder.ConfigureBuilder.ConfigureBuilderSection;
import com.outbrain.ob1k.server.builder.ResourceMappingBuilder.ResourceMappingBuilderSection;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder.ServiceRegisterBuilderSection;
import com.outbrain.ob1k.server.endpoints.EndpointMappingServiceBuilder;

/**
 * Simple server builder that defines an API of
 * one initial step and four optional main sections to build a server.
 *
 * In this context:
 *
 * Step  - is a method that must be called in order for the builder caller to proceed.
 * A Step is implemented by wither a class or an interface that returns the next step
 * (or the builder if there are no more steps) as its return value.
 *
 * Section - is a method that takes a BuilderSection - a Functional Interface that will allow the user to define
 * specific sub-builders for a particular "section" of the builder state.
 * Note that a section can be a step but doesn't have to.
 * A Section will be implemented as a method that takes a (genericized) BuilderSection as an argument.
 *
 * In this builder:
 *
 * The initial step is the ContextPathBuildStep (required by the ServiceRegistry to come before any other step).
 * The sections are:
 *  - configure (for building configuration using ConfigureBuilder)
 *  - service (for building services and registering them using ServiceRegisterBuilder)
 *  - resource (for building resource mapping using ResourceMappingBuilder)
 *  - and (for general extension builders - Anything that will take ServerBuilderState as argument.
 *  see EndpointMappingServiceBuilder for an example of an extension builder.
 *
 *  @see BuilderStep
 *  @see BuilderSection
 *  @see ContextPathBuildStep
 *  @see ConfigureBuilder
 *  @see ServiceRegisterBuilder
 *  @see ResourceMappingBuilder
 *  @see ExtensionBuilder
 *  @see EndpointMappingServiceBuilder
 */
public class ServerBuilder<B extends ServerBuilder<B>> extends AbstractServerBuilder {

  private final ServiceRegisterBuilder service;
  private final ResourceMappingBuilder resource;
  private final ConfigureBuilder configure;
  private final ServerBuilderState state;

  public static ContextPathBuildStep<ServerBuilder> newBuilder() {
    final ServerBuilder builder = new ServerBuilder();
    return new ContextPathBuildStep<>(builder, builder.state);
  }

  protected ServerBuilder() {
    super();
    state = innerState();
    configure = new ConfigureBuilder(state);
    service = new ServiceRegisterBuilder(state);
    resource = new ResourceMappingBuilder(state);
  }

  public B configure(final ConfigureBuilderSection section) {
    section.apply(configure);
    return self();
  }

  public B service(final ServiceRegisterBuilderSection section) {
    section.apply(service);
    return self();
  }

  public B resource(final ResourceMappingBuilderSection section) {
    section.apply(resource);
    return self();
  }

  public B withExtension(final ExtensionBuilder extensionBuilder) {
    extensionBuilder.apply(innerState());
    return self();
  }

  @SuppressWarnings("unchecked")
  protected B self() {
    return (B) this;
  }
}
