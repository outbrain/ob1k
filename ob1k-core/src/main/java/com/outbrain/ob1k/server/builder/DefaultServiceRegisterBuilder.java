package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class DefaultServiceRegisterBuilder {

  private final ServerBuilderState state;
  private final DefaultServiceBindBuilder bindBuilder;

  public DefaultServiceRegisterBuilder(final ServerBuilderState state) {
    this.state = state;
    this.bindBuilder = new DefaultServiceBindBuilder(state);

  }

  public DefaultServiceRegisterBuilder register(final Service service, final String path, final ServiceFilter... filters) {
    return register(service, path, NoOpBuilderProvider.<DefaultServiceBindBuilder>getInstance(), filters);
  }

  public DefaultServiceRegisterBuilder register(final Service service, final String path, final BuilderProvider<DefaultServiceBindBuilder> bindProvider, final ServiceFilter... filters) {
    state.addServiceDescriptor(service, path, filters);
    bindProvider.provide(bindBuilder);
    return this;
  }
}
