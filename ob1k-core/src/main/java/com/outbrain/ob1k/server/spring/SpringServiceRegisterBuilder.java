package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.ExtendableServerBuilder;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceBuilderSection;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceRegisterBuilder<E extends ExtendableServerBuilder<E>> extends ServiceBuilderSection<E, SpringServiceRegisterBuilder<E>> {

  private final ServerBuilderState state;
  private final SpringServiceBindingBuilder<E> bindBuilder;
  private final SpringBeanContext ctx;

  public SpringServiceRegisterBuilder(final E builder, final ServerBuilderState state, final SpringBeanContext ctx) {
    super(builder);
    this.ctx = ctx;
    this.state = state;
    this.bindBuilder = new SpringServiceBindingBuilder<>(builder, this, state, ctx);
  }

  @SafeVarargs
  public final SpringServiceBindingBuilder<E> register(final String ctxName, final Class<? extends Service> serviceType,
                                                       final String path, final Class<? extends ServiceFilter>... filterTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    if (filterTypes != null) {
      for (final Class<? extends ServiceFilter> filterType : filterTypes) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }
    }
    final Service service = ctx.getBean(ctxName, serviceType);
    state.addServiceDescriptor(service, path, filters.toArray(new ServiceFilter[filters.size()]));
    return bindBuilder;
  }

  @Override
  protected SpringServiceRegisterBuilder<E> self() {
    return this;
  }
}
