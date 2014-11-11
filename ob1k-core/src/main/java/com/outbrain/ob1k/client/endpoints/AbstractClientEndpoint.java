package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.TypeHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.ExecutionException;

/**
 * a common parent for sync and async endpoints used by the client to call the remote target.
 *
 * Created by aronen on 4/25/14.
 */
public abstract class AbstractClientEndpoint {
  public final Method method;
  public final Class serviceType;
  public final HttpClient client;
  public final ContentType contentType;
  public final String methodPath;

  protected AbstractClientEndpoint(final Method method, final Class serviceType, final HttpClient client,
                                   final ContentType contentType, final String methodPath) {
    this.method = method;
    this.serviceType = serviceType;
    this.client = client;
    this.contentType = contentType;
    this.methodPath = methodPath;
  }

  protected Type getResType() {
    return TypeHelper.extractReturnType(method);
  }

  public abstract Object invoke(final String remoteTarget, final Object[] params) throws Throwable;

}
