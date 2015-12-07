package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.BindBuilderSection;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceBindingBuilder<E extends ExtendableServerBuilder<E>>  extends BindBuilderSection<E, SpringServiceRegisterBuilder<E>> {

  private final ServerBuilderState state;
  private final SpringBeanContext ctx;

  public SpringServiceBindingBuilder(final E builder, final SpringServiceRegisterBuilder<E> serviceBuilder, final ServerBuilderState state, final SpringBeanContext ctx) {
    super(builder, serviceBuilder);
    this.state = state;
    this.ctx = ctx;
  }

  @SafeVarargs
  public final SpringServiceBindingBuilder<E> endpoint(final String methodName, final String path, final String ctxName,
                                                       final Class<? extends ServiceFilter>... filterTypes) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filterTypes);
  }

  @SafeVarargs
  public final SpringServiceBindingBuilder<E> endpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                                     final Class<? extends ServiceFilter>... filtersTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    for (final Class<? extends ServiceFilter> filterType : filtersTypes) {
      final ServiceFilter filter = ctx.getBean(ctxName, filterType);
      filters.add(filter);
    }

    state.setEndpointBinding(requestMethodType, methodName, path, filters.toArray(new ServiceFilter[filters.size()]));
    return this;
  }
}
