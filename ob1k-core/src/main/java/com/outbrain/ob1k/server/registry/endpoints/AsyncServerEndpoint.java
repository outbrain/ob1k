package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.common.concurrent.ComposableFutureHelper;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.server.ctx.AsyncServerRequestContext;
import com.outbrain.ob1k.server.ctx.DefaultAsyncServerRequestContext;
import com.outbrain.ob1k.server.ResponseHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
* Created by aronen on 4/24/14.
*/
public class AsyncServerEndpoint extends AbstractServerEndpoint {
  public final AsyncFilter[] filters;

  public AsyncServerEndpoint(final Service service, final AsyncFilter[] filters, final Method method, final HttpRequestMethodType requestMethodType, final String[] paramNames) {
    super(service, method, requestMethodType, paramNames);
    this.filters = filters;
  }

  public <T> ComposableFuture<T> invokeAsync(final AsyncServerRequestContext ctx) {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final AsyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final ComposableFuture<T> result = ComposableFutureHelper.cast(filter.handleAsync(ctx.nextPhase()));
      return result;
    } else {
      try {
        @SuppressWarnings("unchecked")
        final ComposableFuture<T> result = ComposableFutureHelper.cast(method.invoke(service, ctx.getParams()));
        return result;
      } catch (final IllegalAccessException | InvocationTargetException e) {
        return ComposableFutures.fromError(e);
      }
    }
  }

  @Override
  public void invoke(final Request request, final Object[] params, final ResponseHandler handler) {
    final AsyncServerRequestContext ctx = new DefaultAsyncServerRequestContext(request, this, params);
    final ComposableFuture<Object> response = invokeAsync(ctx);
    handler.handleAsyncResponse(response);
  }
}
