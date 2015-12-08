package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class ServiceRegisterBuilder {

  private final ServerBuilderState state;
  private final ServiceBindBuilder bindBuilder;

  public ServiceRegisterBuilder(final ServerBuilderState state) {
    this.state = state;
    this.bindBuilder = new ServiceBindBuilder(state);

  }

  public ServiceRegisterBuilder register(final Service service, final String path, final ServiceFilter... filters) {
    return register(service, path, NoOpBuilderProvider.<ServiceBindBuilder>getInstance(), filters);
  }

  public ServiceRegisterBuilder register(final Service service, final String path, final BuilderProvider<ServiceBindBuilder> bindProvider, final ServiceFilter... filters) {
    state.addServiceDescriptor(service, path, filters);
    bindProvider.provide(bindBuilder);
    return this;
  }
}
