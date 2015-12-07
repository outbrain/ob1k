package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.BuilderSection;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

public class EndpointMappingServiceBuilder<E extends ExtendableServerBuilder<E>> extends BuilderSection<E> {

  private final ServerBuilderState state;

  public EndpointMappingServiceBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
  }

  public E registerEndpointMappingService(final String path, final ServiceFilter... filters) {
    state.addServiceDescriptor(new EndpointMappingService(state.getRegistry()), path, filters);
    return backToServerBuilder();
  }
}
