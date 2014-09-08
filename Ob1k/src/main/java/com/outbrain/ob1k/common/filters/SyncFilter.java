package com.outbrain.ob1k.common.filters;

import com.outbrain.ob1k.RequestContext;
import com.outbrain.ob1k.SyncRequestContext;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 4/23/14.
 *
 * filters synchronous requests.
 */
public interface SyncFilter<T, C extends SyncRequestContext> extends ServiceFilter {
  T handleSync(C ctx) throws ExecutionException;
}
