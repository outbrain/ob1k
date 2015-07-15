package com.outbrain.ob1k.client.endpoints;

import static com.outbrain.ob1k.concurrent.ComposableFutures.*;

import com.outbrain.ob1k.client.ctx.AsyncClientRequestContext;
import com.outbrain.ob1k.client.ctx.DefaultAsyncClientRequestContext;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.common.filters.AsyncFilter;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
                             final Endpoint endpoint, final AsyncFilter[] filters) {

    super(httpClient, marshallerRegistry, endpoint);
    this.filters = filters;
  }

  @SuppressWarnings("unchecked")
  public <T> ComposableFuture<T> invokeAsync(final AsyncClientRequestContext ctx) {

    if (filters != null && ctx.getExecutionIndex() < filters.length) {

      final AsyncFilter filter = filters[ctx.getExecutionIndex()];
      return (ComposableFuture<T>) filter.handleAsync(ctx.nextPhase());

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
        return (ComposableFuture<T>) requestBuilder.asResponse();
      }

      // If the client requested to get the <T>, together with the whole response object
      if (isTypedResponse(responseType)) {
        final Type type = ((ParameterizedType) responseType).getActualTypeArguments()[0];
        return (ComposableFuture<T>) requestBuilder.asTypedResponse(type);
      }

      return requestBuilder.asValue(responseType);
    }
  }

  @Override
  public Object invoke(final String remoteTarget, final Object[] params) throws Throwable {

    final DefaultAsyncClientRequestContext ctx = new DefaultAsyncClientRequestContext(remoteTarget, params, this);
    return invokeAsync(ctx);
  }
}
