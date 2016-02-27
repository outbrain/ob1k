package com.outbrain.ob1k.common.endpoints;

import com.outbrain.ob1k.common.concurrent.ComposableFutureHelper;
import rx.Observable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by hyadid on 2/28/16.
 */
public class ServiceEndpointContract {
  public static boolean isAsyncMethod(final Method m) {
    return ComposableFutureHelper.isComposableFuture(m.getReturnType());
  }

  public static boolean isStreamingMethod(final Method m) {
    return m.getReturnType() == Observable.class;
  }

  public static boolean isEndpoint(Method method) {
    int modifiers = method.getModifiers();
    return Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers);
  }
}
