package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.DefaultServiceBindBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceBindingBuilder extends DefaultServiceBindBuilder {

  private final SpringBeanContext ctx;

  public SpringServiceBindingBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state);
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

    endpoint(requestMethodType, methodName, path, filters.toArray(new ServiceFilter[filters.size()]));
    return this;
  }
}
