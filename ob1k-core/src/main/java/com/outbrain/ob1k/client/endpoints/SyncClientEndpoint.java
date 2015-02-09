package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ctx.DefaultSyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.SyncClientRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.marshalling.ContentType;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 4/25/14.
 *
 * handle the sync invocation chain of filters and remote target on the client side
 */
public class SyncClientEndpoint extends AbstractClientEndpoint {
  private final SyncFilter[] filters;

  public SyncClientEndpoint(final Method method, final List<String> methodParams, final Class serviceType, final HttpClient client,
                            final SyncFilter[] filters, final ContentType contentType, final String methodPath, final HttpRequestMethodType requestMethodType) {
    super(method, methodParams, serviceType, client, contentType, methodPath, requestMethodType);
    this.filters = filters;
  }

  public <T> T invokeSync(final SyncClientRequestContext ctx) throws ExecutionException {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final SyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final T result = (T) filter.handleSync(ctx.nextPhase());
      return result;
    } else {
      final ComposableFuture<T> result;
      switch (requestMethodType) {
        case GET:
          result = client.httpGet(ctx.getUrl(), getResType(), contentType.requestEncoding(), methodParamNames, ctx.getParams());;
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
      try {
        return result.get();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new ExecutionException(e);
      }
    }
  }

  @Override
  public Object invoke(final String remoteTarget, final Object[] params) throws Throwable {
    final DefaultSyncClientRequestContext ctx = new DefaultSyncClientRequestContext(remoteTarget, params, this);
    return invokeSync(ctx);
  }
}
