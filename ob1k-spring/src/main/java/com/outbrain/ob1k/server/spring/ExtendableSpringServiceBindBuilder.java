package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder;

import java.util.ArrayList;
import java.util.List;

public class ExtendableSpringServiceBindBuilder<B extends ExtendableSpringServiceBindBuilder<B>>
                extends ServiceBindBuilder<B> {

  private final SpringBeanContext ctx;
  private final ServiceBindBuilder bindBuilder;

  protected ExtendableSpringServiceBindBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state);
    bindBuilder = new ServiceBindBuilder(state);
    this.ctx = ctx;
  }

  @SafeVarargs
  public final ExtendableSpringServiceBindBuilder endpoint(final String methodName, final String path, final String ctxName,
                                                           final Class<? extends ServiceFilter>... filterTypes) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filterTypes);
  }

  @SafeVarargs
  public final ExtendableSpringServiceBindBuilder endpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                                           final Class<? extends ServiceFilter>... filtersTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    for (final Class<? extends ServiceFilter> filterType : filtersTypes) {
      final ServiceFilter filter = ctx.getBean(ctxName, filterType);
      filters.add(filter);
    }

    bindBuilder.endpoint(requestMethodType, methodName, path, filters.toArray(new ServiceFilter[filters.size()]));
    return this;
  }
}
