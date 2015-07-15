package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.client.http.HttpClient;
import com.outbrain.ob1k.client.targets.TargetProvider;
import com.outbrain.ob1k.common.marshalling.ContentType;
import com.outbrain.ob1k.common.marshalling.TypeHelper;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

/**
 * a common parent for sync and async endpoints used by the client to call the remote target.
 *
 * Created by aronen on 4/25/14.
 */
public abstract class AbstractClientEndpoint {
  public final Method method;
  public final List<String> methodParamNames;
  public final Class serviceType;
  public final HttpClient client;
  public final ContentType contentType;
  public final String methodPath;
  public final HttpRequestMethodType requestMethodType;

  protected AbstractClientEndpoint(final Method method, final List<String> methodParamNames, final Class serviceType, final HttpClient client,
                                   final ContentType contentType, final String methodPath, final HttpRequestMethodType requestMethodType) {
    this.method = method;
    this.methodParamNames = methodParamNames;
    this.serviceType = serviceType;
    this.client = client;
    this.contentType = contentType;
    this.methodPath = methodPath;
    this.requestMethodType = requestMethodType;
  }

  protected Type getResType() {
    return TypeHelper.extractReturnType(method);
  }

  public abstract Object invoke(final TargetProvider remoteTarget, final Object[] params) throws Throwable;
}
