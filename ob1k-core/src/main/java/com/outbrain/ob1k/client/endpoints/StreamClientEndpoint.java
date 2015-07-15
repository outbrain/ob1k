package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.ctx.DefaultStreamClientRequestContext;
import com.outbrain.ob1k.client.ctx.StreamClientRequestContext;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.common.marshalling.ContentType;
import rx.Observable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by aronen on 6/10/14.
 *
 * handle the stream based invocation chain of filters and remote target on the client side
 */
public class StreamClientEndpoint extends AbstractClientEndpoint {
  private final StreamFilter[] filters;

  public StreamClientEndpoint(final Method method, final List<String> methodParams, final Class serviceType, final HttpClient client,
                             final StreamFilter[] filters, final ContentType contentType, final String methodPath, final HttpRequestMethodType requestMethodType) {
    super(method, methodParams, serviceType, client, contentType, methodPath, requestMethodType);
    this.filters = filters;
  }

  public <T> Observable<T> invokeStream(final StreamClientRequestContext ctx) {
    if (filters != null && ctx.getExecutionIndex() < filters.length) {
      final StreamFilter filter = filters[ctx.getExecutionIndex()];
      @SuppressWarnings("unchecked")
      final Observable<T> result = (Observable<T>) filter.handleStream(ctx.nextPhase());
      return result;
    } else {
      final Observable<T> result;
      switch (requestMethodType) {
        case GET:
          result = client.httpGetStreaming(ctx.getUrl(), getResType(), contentType.requestEncoding(), methodParamNames, ctx.getParams());
          break;
        case PUT:
          result = client.httpPutStreaming(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
          break;
        case DELETE:
          result = client.httpDeleteStreaming(ctx.getUrl(), getResType(), contentType.requestEncoding(), methodParamNames, ctx.getParams());
          break;
        case POST:
        default:
          result = client.httpPostStreaming(ctx.getUrl(), getResType(), ctx.getParams(), contentType.requestEncoding());
      }
      return result;
    }
  }

  @Override
  public Object invoke(final TargetProvider targetProvider, final Object[] params) throws Throwable {
    final String remoteTarget;
    try{
      remoteTarget = targetProvider.provideTarget();
    } catch (final Throwable t) {
      return Observable.error(t);
    }
    final DefaultStreamClientRequestContext ctx = new DefaultStreamClientRequestContext(remoteTarget, params, this);
    return invokeStream(ctx);
  }

}
