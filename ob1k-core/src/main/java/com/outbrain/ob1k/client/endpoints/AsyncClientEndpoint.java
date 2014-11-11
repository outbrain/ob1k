package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.DefaultAsyncClientRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.marshalling.ContentType;

import java.lang.reflect.Method;

/**
 * Created by aronen on 4/25/14.
 *
 * handle the async invocation chain of filters and remote target on the client side
 */
public class AsyncClientEndpoint extends AbstractClientEndpoint {
  private final AsyncFilter[] filters;

  public AsyncClientEndpoint(final Method method, final Class serviceType, final HttpClient client,
                             final AsyncFilter[] filters, final ContentType contentType, final String methodPath) {
    super(method, serviceType, client, contentType, methodPath);
    this.filters = filters;
  }

  public <T> ComposableFuture<T> invokeAsync(final AsyncClientRequestContext ctx) {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final AsyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final ComposableFuture<T> result = (ComposableFuture<T>) filter.handleAsync(ctx.nextPhase());
      return result;
    } else {
      @SuppressWarnings("unchecked")
      final ComposableFuture<T> result = client.httpPost(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
      return result;
    }
  }

  @Override
  public Object invoke(final String remoteTarget, final Object[] params) throws Throwable {
    final DefaultAsyncClientRequestContext ctx = new DefaultAsyncClientRequestContext(remoteTarget, params, this);
    return invokeAsync(ctx);
  }
}
