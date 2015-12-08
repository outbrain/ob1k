package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.build.BuilderProvider;
import com.outbrain.ob1k.server.build.NoOpBuilderProvider;
import com.outbrain.ob1k.server.build.ServerBuilderState;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceRegisterBuilder {

  private final ServerBuilderState state;
  private final SpringServiceBindingBuilder bindBuilder;
  private final SpringBeanContext ctx;

  public SpringServiceRegisterBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    this.ctx = ctx;
    this.state = state;
    this.bindBuilder = new SpringServiceBindingBuilder(state, ctx);
  }

  @SafeVarargs
  public final SpringServiceRegisterBuilder register(final String ctxName, final Class<? extends Service> serviceType,
                                                     final String path, final Class<? extends ServiceFilter>... filterTypes) {
    return register(ctxName, serviceType, path, NoOpBuilderProvider.<SpringServiceBindingBuilder>getInstance(), filterTypes);
  }

  @SafeVarargs
  public final SpringServiceRegisterBuilder register(final String ctxName, final Class<? extends Service> serviceType,
                                                     final String path, final BuilderProvider<SpringServiceBindingBuilder> bindProvider,
                                                     final Class<? extends ServiceFilter>... filterTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    if (filterTypes != null) {
      for (final Class<? extends ServiceFilter> filterType : filterTypes) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }
    }
    final Service service = ctx.getBean(ctxName, serviceType);
    state.addServiceDescriptor(service, path, filters.toArray(new ServiceFilter[filters.size()]));
    bindProvider.provide(bindBuilder);
    return this;
  }
}
