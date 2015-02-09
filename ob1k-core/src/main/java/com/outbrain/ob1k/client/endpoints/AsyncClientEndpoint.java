package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.DefaultAsyncClientRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.marshalling.ContentType;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by aronen on 4/25/14.
 *
 * handle the async invocation chain of filters and remote target on the client side
 */
public class AsyncClientEndpoint extends AbstractClientEndpoint {
  private final AsyncFilter[] filters;

  public AsyncClientEndpoint(final Method method, final List<String> methodParams, final Class serviceType, final HttpClient client,
                             final AsyncFilter[] filters, final ContentType contentType, final String methodPath, final HttpRequestMethodType requestMethodType) {
    super(method, methodParams, serviceType, client, contentType, methodPath, requestMethodType);
    this.filters = filters;
  }

  public <T> ComposableFuture<T> invokeAsync(final AsyncClientRequestContext ctx) {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final AsyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final ComposableFuture<T> result = (ComposableFuture<T>) filter.handleAsync(ctx.nextPhase());
      return result;
    } else {
      final ComposableFuture<T> result;
      switch (requestMethodType) {
        case GET:
          result = client.httpGet(ctx.getUrl(), getResType(), contentType.requestEncoding(), methodParamNames, ctx.getParams());
          break;
        case PUT:
          result = client.httpPut(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
          break;
        case DELETE:
          result = client.httpDelete(ctx.getUrl(), getResType(), contentType.requestEncoding(), methodParamNames, ctx.getParams());
          break;
        case POST:
        default:
          result = client.httpPost(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
      }
      return result;
    }
  }

  @Override
  public Object invoke(final String remoteTarget, final Object[] params) throws Throwable {
    final DefaultAsyncClientRequestContext ctx = new DefaultAsyncClientRequestContext(remoteTarget, params, this);
    return invokeAsync(ctx);
  }
}
