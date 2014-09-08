package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.common.filters.ServiceFilter;

/**
 * Created by aronen on 7/17/14.
 */
public interface ContextBasedServiceBuilderPhase extends RawServiceBuilderPhase {
  ContextBasedServiceBuilderPhase addEndpoint(final String methodName, final String path);
  ContextBasedServiceBuilderPhase addFilterFromContext(final String ctxName, final Class<? extends ServiceFilter> filterType);
}
