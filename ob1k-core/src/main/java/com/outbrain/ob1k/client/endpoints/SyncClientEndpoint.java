package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.ctx.DefaultSyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.SyncClientRequestContext;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.marshalling.ContentType;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

/**
 * Created by aronen on 4/25/14.
 *
 * handle the sync invocation chain of filters and remote target on the client side
 */
public class SyncClientEndpoint extends AbstractClientEndpoint {
  private final SyncFilter[] filters;

  public SyncClientEndpoint(final Method method, final Class serviceType, final HttpClient client,
                            final SyncFilter[] filters, final ContentType contentType, final String methodPath) {
    super(method, serviceType, client, contentType, methodPath);
    this.filters = filters;
  }

  public <T> T invokeSync(final SyncClientRequestContext ctx) throws ExecutionException {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final SyncFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked") final
      T result = (T) filter.handleSync(ctx.nextPhase());
      return result;
    } else {
      @SuppressWarnings("unchecked")
      final ComposableFuture<T> result = client.httpPost(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
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
