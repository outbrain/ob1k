package com.outbrain.ob1k.concurrent.handlers;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 8/11/14.
 */
@Deprecated
public interface SuccessHandler<T, R> {
  R handle(T result) throws ExecutionException;
}
