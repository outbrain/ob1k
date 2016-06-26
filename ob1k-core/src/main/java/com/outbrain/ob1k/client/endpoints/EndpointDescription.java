package com.outbrain.ob1k.client.endpoints;

import com.outbrain.ob1k.HttpRequestMethodType;
import com.outbrain.ob1k.http.common.ContentType;

import java.lang.reflect.Method;

/**
 * Describes client endpoint properties
 *
 * @author marenzon
 */
public class EndpointDescription {

  private final Method method;
  private final Class serviceType;
  private final ContentType contentType;
  private final String methodPath;
  private final HttpRequestMethodType requestMethodType;

  public EndpointDescription(final Method method, final Class serviceType, final ContentType contentType,
                             final String methodPath, final HttpRequestMethodType requestMethodType) {
    this.method = method;
    this.serviceType = serviceType;
    this.contentType = contentType;
    this.methodPath = methodPath;
    this.requestMethodType = requestMethodType;
  }

  public Method getMethod() {
    return method;
  }

  public Class getServiceType() {
    return serviceType;
  }

  public ContentType getContentType() {
    return contentType;
  }

  public String getMethodPath() {
    return methodPath;
  }

  public HttpRequestMethodType getRequestMethodType() {
    return requestMethodType;
  }
}