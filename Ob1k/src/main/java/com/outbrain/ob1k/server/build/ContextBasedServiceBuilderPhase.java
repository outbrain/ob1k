package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.common.filters.ServiceFilter;

import java.util.List;

/**
 * Created by aronen on 7/17/14.
 */
public interface ContextBasedServiceBuilderPhase extends RawServiceBuilderPhase {
  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path);

  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName,
                                              final Class<? extends ServiceFilter> filterType);

  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path, final String ctxName,
                                              final List<Class<? extends ServiceFilter>> filtersType);
}
