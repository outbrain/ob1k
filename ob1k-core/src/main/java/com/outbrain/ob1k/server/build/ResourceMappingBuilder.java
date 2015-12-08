package com.outbrain.ob1k.server.build;

public class ResourceMappingBuilder<B extends ResourceMappingBuilder<B>> {

  private final ServerBuilderState state;

  public ResourceMappingBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public B staticResource(final String mapping, final String location) {
    state.addStaticResource(mapping, location);
    return self();
  }

  public B staticPath(final String path) {
    state.addStaticFolder(path);
    return self();
  }

  public B staticMapping(final String virtualPath, final String realPath) {
    state.addStaticMapping(virtualPath, realPath);
    return self();
  }

  @SuppressWarnings("unchecked")
  private B self() {
    return (B) this;
  }
}
