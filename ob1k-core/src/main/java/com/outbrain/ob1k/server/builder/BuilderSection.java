package com.outbrain.ob1k.server.builder;

/**
 * A functional interface that defines a section for the builder.
 *
 * It allows callers to builder sections to receive a sub-builder (T) for a specific "section of the overall
 * builder and call it to build the server.
 *
 * For example, a builder can implement the method
 *
 *    resourceMapping(final BuilderSection<ResourceMappingBuilder>)
 *
 * and specify a ResourceMappingBuilder that will only contain the portion of the builder methods specific
 * for resource mapping.
 *
 * The caller will use this by calling:
 *
 *   builder.resourceMapping(resource -> resource.staticPath("css").virtualMap("foo", "bar"))...
 *
 *   etc.
 *
 * @param <T> sub builder for this section
 */
public interface BuilderSection<T> {

  void apply(final T builder);
}
