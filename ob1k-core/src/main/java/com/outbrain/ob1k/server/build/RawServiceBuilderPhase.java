package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.common.filters.ServiceFilter;

/**
 * Created by aronen on 7/17/14.
 */
@Deprecated // use new extendable fluent builder in 'builder' package
public interface RawServiceBuilderPhase {
  RawServiceBuilderPhase addEndpoint(final String methodName, final String path, ServiceFilter... filters);
  RawServiceBuilderPhase addEndpoint(final HttpRequestMethodType requestMethodType, final String methodName, final String path, ServiceFilter... filters);
}