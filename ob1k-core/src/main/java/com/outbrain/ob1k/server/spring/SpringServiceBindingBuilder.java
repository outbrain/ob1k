package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.ServerBuilderState;
import com.outbrain.ob1k.server.build.ServiceBindBuilder;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceBindingBuilder {

  private final SpringBeanContext ctx;
  private final ServiceBindBuilder bindBuilder;

  public SpringServiceBindingBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    bindBuilder = new ServiceBindBuilder(state);
    this.ctx = ctx;
  }

  @SafeVarargs
  public final SpringServiceBindingBuilder endpoint(final String methodName, final String path, final String ctxName,
                                                       final Class<? extends ServiceFilter>... filterTypes) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filterTypes);
  }

  @SafeVarargs
  public final SpringServiceBindingBuilder endpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                                     final Class<? extends ServiceFilter>... filtersTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    for (final Class<? extends ServiceFilter> filterType : filtersTypes) {
      final ServiceFilter filter = ctx.getBean(ctxName, filterType);
      filters.add(filter);
    }

    bindBuilder.endpoint(requestMethodType, methodName, path, filters.toArray(new ServiceFilter[filters.size()]));
    return this;
  }

  public ServiceBindBuilder bindPrefix(final boolean bindPrefix) {
    return bindBuilder.bindPrefix(bindPrefix);
  }

  public ServiceBindBuilder endpoint(final HttpRequestMethodType methodType, final String methodName, final String path, final ServiceFilter... filters) {
    return bindBuilder.endpoint(methodType, methodName, path, filters);
  }

  public ServiceBindBuilder endpoint(final String methodName, final String path, final ServiceFilter... filters) {
    return bindBuilder.endpoint(methodName, path, filters);
  }
}
