package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class DefaultServiceBindBuilder<E extends ExtendableServerBuilder<E>, S>  extends BindBuilderSection<E, S> {

  private final ServerBuilderState state;

  public DefaultServiceBindBuilder(final E builder, final S serviceBuilder, final ServerBuilderState state) {
    super(builder, serviceBuilder);
    this.state = state;
  }

  public DefaultServiceBindBuilder<E, S> bindPrefix(final boolean bindPrefix) {
    state.setBindPrefixToLastDescriptor(bindPrefix);
    return this;
  }

  public DefaultServiceBindBuilder<E, S> endpoint(final String methodName, final String path, final ServiceFilter... filters) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, filters);
  }

  public DefaultServiceBindBuilder<E, S> endpoint(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter... filters) {
    state.setEndpointBinding(methodType, methodName, path, filters);
    return this;
  }
}
