package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.BuilderSection;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceRegisterBuilder;
import com.outbrain.ob1k.server.spring.SpringServiceBindBuilder.SpringServiceBindBuilderSection;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceRegisterBuilder extends ServiceRegisterBuilder<SpringServiceRegisterBuilder> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface SpringServiceRegisterBuilderSection extends BuilderSection<SpringServiceRegisterBuilder> {}

  private static final NoOpBindSection NO_OP = new NoOpBindSection();
  private final SpringServiceBindBuilder bindBuilder;
  private final SpringBeanContext ctx;

  public SpringServiceRegisterBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state);
    this.ctx = ctx;
    this.bindBuilder = new SpringServiceBindBuilder(state, ctx);
  }

  @SafeVarargs
  public final SpringServiceRegisterBuilder register(final String ctxName, final Class<? extends Service> serviceType,
                                                     final String path, final Class<? extends ServiceFilter>... filterTypes) {
    return register(ctxName, serviceType, path, NO_OP, filterTypes);
  }

  @SafeVarargs
  public final SpringServiceRegisterBuilder register(final String ctxName, final Class<? extends Service> serviceType,
                                                     final String path, final SpringServiceBindBuilderSection bindSection,
                                                     final Class<? extends ServiceFilter>... filterTypes) {
    final List<ServiceFilter> filters = new ArrayList<>();
    if (filterTypes != null) {
      for (final Class<? extends ServiceFilter> filterType : filterTypes) {
        final ServiceFilter filter = ctx.getBean(ctxName, filterType);
        filters.add(filter);
      }
    }
    final Service service = ctx.getBean(ctxName, serviceType);
    register(service, path, filters.toArray(new ServiceFilter[filters.size()]));
    bindSection.apply(bindBuilder);
    return this;
  }

  private static class NoOpBindSection implements SpringServiceBindBuilderSection {

    @Override
    public void apply(final SpringServiceBindBuilder builder) {
      // do nothing
    }
  }
}
