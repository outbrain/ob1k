package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.ctx.DefaultStreamClientRequestContext;
import com.outbrain.ob1k.client.ctx.StreamClientRequestContext;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.marshalling.ContentType;
import rx.Observable;

import java.lang.reflect.Method;

/**
 * Created by aronen on 6/10/14.
 *
 * handle the stream based invocation chain of filters and remote target on the client side
 */
public class StreamClientEndpoint extends AbstractClientEndpoint {
  private final StreamFilter[] filters;

  public StreamClientEndpoint(final Method method, final Class serviceType, final HttpClient client,
                             final StreamFilter[] filters, final ContentType contentType, final String methodPath) {
    super(method, serviceType, client, contentType, methodPath);
    this.filters = filters;
  }

  public <T> Observable<T> invokeStream(final StreamClientRequestContext ctx) {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final StreamFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final Observable<T> result = (Observable<T>) filter.handleStream(ctx.nextPhase());
      return result;
    } else {
      @SuppressWarnings("unchecked")
      final Observable<T> result = client.httpPostStreaming(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
      return result;
    }

  }

  @Override
  public Object invoke(final String remoteTarget, final Object[] params) throws Throwable {
    final DefaultStreamClientRequestContext ctx = new DefaultStreamClientRequestContext(remoteTarget, params, this);
    return invokeStream(ctx);
  }

}
