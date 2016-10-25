package com.outbrain.ob1k.concurrent.handlers;

import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 8/11/14.
 */
@Deprecated
public interface ErrorHandler<R> {
  R handle(Throwable error) throws ExecutionException;
}
