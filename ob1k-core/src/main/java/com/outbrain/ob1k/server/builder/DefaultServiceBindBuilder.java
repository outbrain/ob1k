package com.outbrain.ob1k.server.builder;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class DefaultServiceBindBuilder {

  private final ServerBuilderState state;

  public DefaultServiceBindBuilder( final ServerBuilderState state) {
    this.state = state;
  }

  public DefaultServiceBindBuilder bindPrefix(final boolean bindPrefix) {
    state.setBindPrefixToLastDescriptor(bindPrefix);
    return this;
  }

  public DefaultServiceBindBuilder endpoint(final String methodName, final String path, final ServiceFilter... filters) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, filters);
  }

  public DefaultServiceBindBuilder endpoint(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter... filters) {
    state.setEndpointBinding(methodType, methodName, path, filters);
    return this;
  }
}
