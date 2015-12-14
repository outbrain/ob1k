package com.outbrain.ob1k.server.spring;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;
import com.outbrain.ob1k.server.builder.BuilderSection;
import com.outbrain.ob1k.server.builder.ServerBuilderState;
import com.outbrain.ob1k.server.builder.ServiceBindBuilder;

import java.util.ArrayList;
import java.util.List;

public class SpringServiceBindBuilder extends ServiceBindBuilder<SpringServiceBindBuilder> {

  /**
   * This non generic interface is used to bypass a Java 8 lambda compiler issue
   * where the compiler fails to infer the lambda argument if those are generic type in a method
   * for a genericized class.
   */
  public interface SpringServiceBindBuilderSection extends BuilderSection<SpringServiceBindBuilder> {}

  private final SpringBeanContext ctx;
  private final ServiceBindBuilder bindBuilder;

  public SpringServiceBindBuilder(final ServerBuilderState state, final SpringBeanContext ctx) {
    super(state);
    bindBuilder = new ServiceBindBuilder(state);
    this.ctx = ctx;
  }

  @SafeVarargs
  public final SpringServiceBindBuilder endpoint(final String methodName, final String path, final String ctxName,
                                                 final Class<? extends ServiceFilter>... filterTypes) {
    return endpoint(HttpRequestMethodType.ANY, methodName, path, ctxName, filterTypes);
  }

  @SafeVarargs
  public final SpringServiceBindBuilder endpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
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
