package com.outbrain.ob1k.server.registry.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.server.ResponseHandler;
import com.outbrain.ob1k.server.ctx.DefaultStreamServerRequestContext;
import com.outbrain.ob1k.server.ctx.StreamServerRequestContext;
import rx.Observable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by aronen on 6/9/14.
 *
 * a server endpoint that knows how to call a stream based method.
 */
public class StreamServerEndpoint extends AbstractServerEndpoint<StreamFilter> {

  public StreamServerEndpoint(final Service service, final StreamFilter[] filters, final Method method, final HttpRequestMethodType requestMethodType, final String[] paramNames) {
    super(service, method, requestMethodType, paramNames, filters);
  }

  public <T> Observable<T> invokeStream(final StreamServerRequestContext ctx) {
    final StreamFilter[] filters = getFilters();
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final StreamFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final Observable<T> result = (Observable<T>) filter.handleStream(ctx.nextPhase());
      return result;
    } else {
      try {
        @SuppressWarnings("unchecked")
        final Observable<T> result = (Observable<T>) invokeMethodOnService(ctx.getParams());
        return result;
      } catch (final IllegalAccessException | InvocationTargetException e) {
        return Observable.error(e);
      }
    }

  }

  @Override
  public void invoke(final Request request, final Object[] params, final ResponseHandler handler) {
    final StreamServerRequestContext ctx = new DefaultStreamServerRequestContext(request, this, params);
    final Observable<Object> response = invokeStream(ctx);
    final boolean rawStream = request.getQueryParam("_useRawStream_", "false").equals("true");
    handler.handleStreamResponse(response, rawStream);
  }

}
