package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.ctx.DefaultStreamClientRequestContext;
import com.outbrain.ob1k.client.ctx.StreamClientRequestContext;
import com.outbrain.ob1k.client.dispatch.DispatchStrategy;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.http.HttpClient;

import com.outbrain.ob1k.common.filters.StreamFilter;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.Response;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.apache.commons.codec.EncoderException;
import rx.Observable;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Handle the stream based invocation chain of filters and remote target on the client side
 *
 * @author aronen
 */
public class StreamClientEndpoint extends AbstractClientEndpoint {

  private final StreamFilter[] filters;
  private final MarshallingStrategy marshallingStrategy = new MarshallingStrategy() {
    @Override
    public <T> T unmarshall(final Type type, final Response response) throws IOException {
      final RequestMarshaller marshaller = marshallerRegistry.getMarshaller(response.getContentType());
      return marshaller.unmarshallStreamResponse(response, type);
    }
    @Override
    public byte[] marshall(final Object value) throws IOException {
      return marshallObject(value);
    }
  };

  public StreamClientEndpoint(final HttpClient httpClient, final RequestMarshallerRegistry marshallerRegistry,
                              final Endpoint endpoint, final StreamFilter[] filters) {

    super(httpClient, marshallerRegistry, endpoint);
    this.filters = filters;
  }

  @SuppressWarnings("unchecked")
  public <T> Observable<T> invokeStream(final StreamClientRequestContext ctx) {

    if (filters != null && ctx.getExecutionIndex() < filters.length) {

      final StreamFilter filter = filters[ctx.getExecutionIndex()];
      return (Observable<T>) filter.handleStream(ctx.nextPhase());

    } else {

      final RequestBuilder requestBuilder;

      try {
        requestBuilder = buildEndpointRequestBuilder(ctx, marshallingStrategy);
      } catch (final EncoderException | IOException e) {
        return Observable.error(e);
      }

      final Type responseType = extractResponseType();

      // If the client requested to get the response object
      if (responseType == Response.class) {
        return (Observable<T>) requestBuilder.asStream();
      }

      // If the client requested to get the <T>, together with the whole response object
      if (isTypedResponse(responseType)) {
        final Type type = ((ParameterizedType) responseType).getActualTypeArguments()[0];
        return (Observable<T>) requestBuilder.asTypedStream(type);
      }

      return requestBuilder.asStreamValue(responseType);
    }
  }

  @Override
  public DispatchAction createDispatchAction(final Object[] params) {
    return remoteTarget -> {
      final DefaultStreamClientRequestContext ctx = new DefaultStreamClientRequestContext(remoteTarget, params, this);
      return invokeStream(ctx);
    };
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object dispatch(final TargetProvider targetProvider, final DispatchStrategy dispatchStrategy,
                         final DispatchAction dispatchAction) {
    return dispatchStrategy.dispatchStream(targetProvider, dispatchAction);
  }
}
