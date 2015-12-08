package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.ExtensionBuilder;
import com.outbrain.ob1k.server.build.ServerBuilderState;

public class EndpointMappingServiceProvider implements ExtensionBuilder {

  private final String path;
  private final ServiceFilter[] filters;

  public static EndpointMappingServiceProvider registerMappingService(final String path, final ServiceFilter... filters) {
    return new EndpointMappingServiceProvider(path, filters);
  }

  private EndpointMappingServiceProvider(final String path, final ServiceFilter... filters) {
    this.path = path;
    this.filters = filters;
  }

  @Override
  public void provide(final ServerBuilderState state) {
    state.addServiceDescriptor(new EndpointMappingService(state.getRegistry()), path, filters);
  }
}
