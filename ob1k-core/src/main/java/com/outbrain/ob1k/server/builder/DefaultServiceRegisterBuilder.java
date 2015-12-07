package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class DefaultServiceRegisterBuilder<E extends ExtendableServerBuilder<E>> extends ServiceBuilderSection<E, DefaultServiceRegisterBuilder<E>> {

  private final ServerBuilderState state;
  private final DefaultServiceBindBuilder<E, DefaultServiceRegisterBuilder<E>> bindBuilder;

  public DefaultServiceRegisterBuilder(final E builder, final ServerBuilderState state) {
    super(builder);
    this.state = state;
    this.bindBuilder = new DefaultServiceBindBuilder<>(builder, this, state);

  }

  public DefaultServiceBindBuilder<E, DefaultServiceRegisterBuilder<E>> register(final Service service, final String path, final ServiceFilter... filters) {
    state.addServiceDescriptor(service, path, filters);
    return bindBuilder;
  }

  @Override
  protected DefaultServiceRegisterBuilder<E> self() {
    return this;
  }
}
