package com.outbrain.ob1k;

import rx.Observable;

/**
 * Created by aronen on 6/9/14.
 *
 * a stream based request context
 */
public interface StreamRequestContext extends RequestContext {
  <T> Observable<T> invokeStream();
}
