package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.client.dispatch.DispatchStrategy;
import com.outbrain.ob1k.client.ctx.ClientRequestContext;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.marshalling.RequestMarshaller;
import com.outbrain.ob1k.common.marshalling.RequestMarshallerRegistry;
import com.outbrain.ob1k.common.marshalling.TypeHelper;
import com.outbrain.ob1k.http.HttpClient;
import com.outbrain.ob1k.http.RequestBuilder;
import com.outbrain.ob1k.http.TypedResponse;
import com.outbrain.ob1k.http.marshalling.MarshallingStrategy;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A common parent for async and stream based endpoints used by the client to call the remote target.
 *
 * @author aronen
 */
public abstract class AbstractClientEndpoint {

  protected final RequestMarshallerRegistry marshallerRegistry;
  protected final HttpClient httpClient;
  protected final EndpointDescription endpointDescription;

  protected AbstractClientEndpoint(final HttpClient httpClient, final RequestMarshallerRegistry marshallerRegistry,
                                   final EndpointDescription endpointDescription) {
    this.httpClient = httpClient;
    this.marshallerRegistry = marshallerRegistry;
    this.endpointDescription = endpointDescription;
  }

  protected Type extractResponseType() {
    return TypeHelper.extractReturnType(endpointDescription.getMethod());
  }

  protected RequestBuilder buildEndpointRequestBuilder(final ClientRequestContext ctx,
                                                       final MarshallingStrategy marshallingStrategy)
                                                       throws IOException,EncoderException {
    return ClientEndpointRequestBuilder.build(httpClient, endpointDescription, ctx, marshallingStrategy);
  }

  protected boolean isTypedResponse(final Type responseType) {
    return responseType instanceof ParameterizedType
      && ((ParameterizedType) responseType).getRawType() == TypedResponse.class;
  }

  protected byte[] marshallObject(final Object value) throws IOException {
    final RequestMarshaller marshaller = marshallerRegistry.getMarshaller(endpointDescription.getContentType().requestEncoding());
    return marshaller.marshallRequestParams((Object[]) value);
  }

  public EndpointDescription getEndpointDescription() {
    return endpointDescription;
  }

  public abstract DispatchAction createDispatchAction(final Object[] params);
  public abstract Object dispatch(final TargetProvider targetProvider, final DispatchStrategy dispatchStrategy,
                                  final DispatchAction dispatchAction);
}
