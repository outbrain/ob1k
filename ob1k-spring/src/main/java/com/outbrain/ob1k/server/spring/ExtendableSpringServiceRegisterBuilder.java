package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder;
import com.outbrain.ob1k.server.spring.SpringServiceBindBuilder.SpringServiceBindBuilderSection;

import java.util.ArrayList;
import java.util.List;

public class ExtendableSpringServiceRegisterBuilder<B extends ExtendableSpringServiceRegisterBuilder<B>>
        extends ServiceRegisterBuilder<B> {

  private static final NoOpBindSection NO_OP = new NoOpBindSection();
  private final SpringServiceBindBuilder bindBuilder;
  private final SpringBeanContext ctx;

  protected ExtendableSpringServiceRegisterBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state);
    this.ctx = ctx;
    this.bindBuilder = new SpringServiceBindBuilder(state, ctx);
  }

  public final B register(final String ctxName, final Class<? extends Service> serviceType,
                                                               final String path) {
    return register(ctxName, serviceType, path, NO_OP);
  }

  public final B register(final String ctxName, final Class<? extends Service> serviceType,
                                                               final String path, final SpringServiceBindBuilderSection bindSection) {
    final Service service = ctx.getBean(ctxName, serviceType);
    register(service, path);
    bindSection.apply(bindBuilder);
    return self();
  }

  @SafeVarargs
  public final B withFilters(final String ctxName, final Class<? extends ServiceFilter>... filterTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    if (filterTypes != null) {
      for (final Class<? extends ServiceFilter> filterType : filterTypes) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }
    }
    return withFilters(filters.toArray(new ServiceFilter[filters.size()]));
  }

  private static class NoOpBindSection implements SpringServiceBindBuilderSection {

    @Override
    public void apply(final SpringServiceBindBuilder builder) {
      // do nothing
    }
  }
}
