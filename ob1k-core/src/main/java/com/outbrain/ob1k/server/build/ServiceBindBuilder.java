package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

public class ServiceBindBuilder {

  private final ServerBuilderState state;

  public ServiceBindBuilder(final ServerBuilderState state) {
    this.state = state;
  }

  public ServiceBindBuilder bindPrefix(final boolean bindPrefix) {
    state.setBindPrefixToLastDescriptor(bindPrefix);
    return this;
  }

  public ServiceBindBuilder endpoint(final String methodName, final String path, final ServiceFilter... filters) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, filters);
  }

  public ServiceBindBuilder endpoint(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter... filters) {
    state.setEndpointBinding(methodType, methodName, path, filters);
    return this;
  }


}
