package com.outbrain.ob1k.common.filters;

import com.outbrain.ob1k.StreamRequestContext;
import rx.Observable;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 6/10/14.
 *
 * filters stream requests.
 */
public interface StreamFilter <T, C extends StreamRequestContext> extends ServiceFilter {
  Observable<T> handleStream(C ctx);
}