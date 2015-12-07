package com.outbrain.ob1k.server.builder;

public class DefaultResourceMappingBuilder<E extends ExtendableServerBuilder<E>> extends BuilderSection<E> {

  private final ServerBuilderState state;

  public DefaultResourceMappingBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
  }

  public DefaultResourceMappingBuilder<E> staticResource(final String mapping, final String location) {
    state.addStaticResource(mapping, location);
    return this;
  }

  public DefaultResourceMappingBuilder<E> staticPath(final String path) {
    state.addStaticFolder(path);
    return this;
  }

  public DefaultResourceMappingBuilder<E> staticMapping(final String virtualPath, final String realPath) {
    state.addStaticMapping(virtualPath, realPath);
    return this;
  }
}
