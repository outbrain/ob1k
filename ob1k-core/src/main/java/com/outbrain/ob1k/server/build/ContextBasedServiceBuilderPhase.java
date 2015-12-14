package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

import java.util.List;

/**
 * Created by aronen on 7/17/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface ContextBasedServiceBuilderPhase extends RawServiceBuilderPhase {
  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path);

  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName,
                                              final Class<? extends ServiceFilter> filterType);

  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName,
                                              final List<Class<? extends ServiceFilter>> filtersType);

  ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path);

  ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                              final Class<? extends ServiceFilter> filterType);

  ContextBasedServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, final String ctxName,
                                              final List<Class<? extends ServiceFilter>> filtersType);
}