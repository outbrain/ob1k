package com.outbrain.ob1k.server.builder;

public class DefaultResourceMappingBuilder {

  private final ServerBuilderState state;

  public DefaultResourceMappingBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public DefaultResourceMappingBuilder staticResource(final String mapping, final String location) {
    state.addStaticResource(mapping, location);
    return this;
  }

  public DefaultResourceMappingBuilder staticPath(final String path) {
    state.addStaticFolder(path);
    return this;
  }

  public DefaultResourceMappingBuilder staticMapping(final String virtualPath, final String realPath) {
    state.addStaticMapping(virtualPath, realPath);
    return this;
  }
}
