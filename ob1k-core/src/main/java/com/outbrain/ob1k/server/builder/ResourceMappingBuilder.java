package com.outbrain.ob1k.server.builder;

/**
 * portion of the ServerBuilder methods used for resource mapping.
 *
 * static path, resources etc.
 *
 *
 * @param <B> ability to extend this builder
 */
public class ResourceMappingBuilder<B extends ResourceMappingBuilder<B>> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface ResourceMappingBuilderSection extends BuilderSection<ResourceMappingBuilder> {}

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
