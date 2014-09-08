package com.outbrain.ob1k.server.build;

import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.filters.SyncFilter;

/**
 * Created by aronen on 7/17/14.
 */
public interface RawServiceBuilderPhase {
  RawServiceBuilderPhase addEndpoint(final String methodName, final String path);

  RawServiceBuilderPhase addFilter(final AsyncFilter filter);
  RawServiceBuilderPhase addFilter(final SyncFilter filter);
  RawServiceBuilderPhase addFilter(final StreamFilter filter);
}
