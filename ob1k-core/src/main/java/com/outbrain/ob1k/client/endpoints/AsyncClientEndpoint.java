package com.outbrain.ob1k.client.endpoints;

import static com.outbrain.ob1k.concurrent.ComposableFutures.*;

import com.outbrain.ob1k.client.DoubleDispatchStrategy;
import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.DefaultAsyncClientRequestContext;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.concurrent.ComposableFutureHelper;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.concurrent.handlers.FutureAction;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * Handle the async invocation chain of filters and remote target on the client side
 *
 * @author aronen
 */
public class AsyncClientEndpoint extends AbstractClientEndpoint {

  private final AsyncFilter[] filters;
  private final MarshallingStrategy marshallingStrategy = new MarshallingStrategy() {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T unmarshall(final Type type, final Response response) throws IOException {
      final RequestMarshaller marshaller = marshallerRegistry.getMarshaller(response.getContentType());
      return marshaller.unmarshallResponse(response, type);
    }
    @Override
    public byte[] marshall(final Object value) throws IOException {
      return marshallObject(value);
    }
  };

  public AsyncClientEndpoint(final HttpClient httpClient, final RequestMarshallerRegistry marshallerRegistry,
                             final Endpoint endpoint, final AsyncFilter[] filters,
                             final DoubleDispatchStrategy doubleDispatchStrategy) {

    super(httpClient, marshallerRegistry, endpoint, doubleDispatchStrategy);
    this.filters = filters;
  }

  @SuppressWarnings("unchecked")
  public <T> ComposableFuture<T> invokeAsync(final AsyncClientRequestContext ctx) {

    if (filters != null && ctx.getExecutionIndex() < filters.length) {

      final AsyncFilter filter = filters[ctx.getExecutionIndex()];
      return ComposableFutureHelper.cast(filter.handleAsync(ctx.nextPhase()));

    } else {

      final RequestBuilder requestBuilder;

      try {
        requestBuilder = buildEndpointRequestBuilder(ctx, marshallingStrategy);
      } catch (final EncoderException | IOException e) {
        return fromError(e);
      }

      final Type responseType = extractResponseType();

      // If the client requested to get the response object
      if (responseType == Response.class) {
        return ComposableFutureHelper.cast(requestBuilder.asResponse());
      }

      // If the client requested to get the <T>, together with the whole response object
      if (isTypedResponse(responseType)) {
        final Type type = ((ParameterizedType) responseType).getActualTypeArguments()[0];
        return ComposableFutureHelper.cast(requestBuilder.asTypedResponse(type));
      }

      return requestBuilder.asValue(responseType);
    }
  }

  private class InvokeAsyncAction<T> implements FutureAction<T> {
    private final TargetProvider targetProvider;
    private final AsyncClientEndpoint asyncClientEndpoint;
    private final Object[] params;
    private final DoubleDispatchStrategy doubleDispatchStrategy;
    private volatile String firstInvocationTarget;

    public InvokeAsyncAction(final TargetProvider targetProvider, final Object[] params, final DoubleDispatchStrategy doubleDispatchStrategy, final AsyncClientEndpoint asyncClientEndpoint) {
      this.targetProvider=targetProvider;
      this.asyncClientEndpoint = asyncClientEndpoint;
      this.doubleDispatchStrategy = doubleDispatchStrategy;
      this.params = params;
    }

    @Override
    public ComposableFuture<T> execute() {
      final String remoteTarget;
      try{
        remoteTarget = provideTarget();
      } catch (final RuntimeException e) {
        return fromError(e);
      }
      final DefaultAsyncClientRequestContext ctx = new DefaultAsyncClientRequestContext(remoteTarget, params, asyncClientEndpoint);
      final long startTime = System.currentTimeMillis();
      final ComposableFuture<T> result = asyncClientEndpoint.invokeAsync(ctx);
      if (doubleDispatchStrategy != null) {
        result.consume((res) -> doubleDispatchStrategy.onComplete(res, startTime));
      }
      return result;
    }

    private String provideTarget() {
      String remoteTarget;
      remoteTarget = targetProvider.provideTarget();
      if (firstInvocationTarget != null) {
        if (firstInvocationTarget.equals(remoteTarget)) {
          remoteTarget = targetProvider.provideTarget(); // try providing another target. Target provider is thread local and it will ensure that it will provide a different target if possible
        }
      } else {
        firstInvocationTarget = remoteTarget;
      }
      return remoteTarget;
    }
  }

  @Override
  public Object invoke(final TargetProvider targetProvider, final Object[] params) throws Throwable {
    final InvokeAsyncAction action = new InvokeAsyncAction(targetProvider, params, doubleDispatchStrategy, this);
    if (doubleDispatchStrategy != null) {
      return ComposableFutures.doubleDispatch(doubleDispatchStrategy.getDoubleDispatchIntervalMs(), TimeUnit.MILLISECONDS, action);
    } else {
      return action.execute();
    }
 }
}
