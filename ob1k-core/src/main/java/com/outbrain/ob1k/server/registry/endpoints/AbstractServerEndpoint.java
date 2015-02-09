package com.outbrain.ob1k.server.registry.endpoints;

import com.google.common.base.Joiner;
import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.server.ResponseHandler;

import java.lang.reflect.Method;

/**
 * Created by aronen on 4/24/14.
*/
public abstract class AbstractServerEndpoint {

  private static final String TARGET_FORMAT = "%s.%s(%s)";

  public final Service service;
  public final Method method;
  public final HttpRequestMethodType requestMethodType;
  public final String[] paramNames;

  public AbstractServerEndpoint(final Service service, final Method method, final HttpRequestMethodType requestMethodType, final String[] paramNames) {
    this.service = service;
    this.method = method;
    this.requestMethodType = requestMethodType;
    this.paramNames = paramNames;
  }

  public String getTargetAsString() {
    return String.format(TARGET_FORMAT, service.getClass().getName(), method.getName(), Joiner.on(", ").join(paramNames));
  }

  public abstract void invoke(final Request request, final Object[] params, final ResponseHandler handler);
}
