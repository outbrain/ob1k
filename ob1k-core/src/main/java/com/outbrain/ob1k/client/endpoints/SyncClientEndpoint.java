package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.ctx.DefaultSyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.SyncClientRequestContext;

import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.SyncFilter;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

/**
 * Handle the sync invocation chain of filters and remote target on the client side
 *
 * @author aronen
 */
public class SyncClientEndpoint extends AbstractClientEndpoint {

  private final SyncFilter[] filters;
  private final MarshallingStrategy marshallingStrategy = new MarshallingStrategy() {
    @Override
    public <T> T unmarshall(final Type type, final Response response) throws IOException {
      final RequestMarshaller marshaller = marshallerRegistry.getMarshaller(response.getContentType());
      return marshaller.unmarshallResponse(response, type);
    }
    @Override
    public byte[] marshall(final Object value) throws IOException {
      return marshallObject(value);
    }
  };

  public SyncClientEndpoint(final HttpClient httpClient, final RequestMarshallerRegistry marshallerRegistry,
                            final Endpoint endpoint, final SyncFilter[] filters) {

    super(httpClient, marshallerRegistry, endpoint);
    this.filters = filters;
  }

  @SuppressWarnings("unchecked")
  public <T> T invokeSync(final SyncClientRequestContext ctx) throws ExecutionException {

    if (filters != null && ctx.getExecutionIndex() < filters.length) {

      final SyncFilter filter = filters[ctx.getExecutionIndex()];
      return (T) filter.handleSync(ctx.nextPhase());

    } else {

      final RequestBuilder requestBuilder;

      try {
        requestBuilder = buildEndpointRequestBuilder(ctx, marshallingStrategy);
      } catch (final EncoderException | IOException e) {
        throw new ExecutionException(e);
      }

      final Type responseType = extractResponseType();

      try {

        // If the client requested to get the response object
        if (responseType == Response.class) {
          return (T) requestBuilder.asResponse().get();
        }

        // If the client requested to get the <T>, together with the whole response object
        if (isTypedResponse(responseType)) {
          final Type type = ((ParameterizedType) responseType).getActualTypeArguments()[0];
          return (T) requestBuilder.asTypedResponse(type).get();
        }

        final ComposableFuture<T> response = requestBuilder.asValue(responseType);
        return response.get();

      } catch (final InterruptedException e) {

        Thread.currentThread().interrupt();
        throw new ExecutionException(e);
      }
    }
  }

  @Override
  public Object invoke(final TargetProvider targetProvider, final Object[] params) throws Throwable {
    final String remoteTarget = targetProvider.provideTarget();

    final DefaultSyncClientRequestContext ctx = new DefaultSyncClientRequestContext(remoteTarget, params, this);
    return invokeSync(ctx);
  }
}
