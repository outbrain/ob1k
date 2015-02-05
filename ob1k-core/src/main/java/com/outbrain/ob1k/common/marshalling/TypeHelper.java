package com.outbrain.ob1k.common.marshalling;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * User: aronen
 * Date: 8/18/13
 * Time: 7:41 PM
 */
public class TypeHelper {
  public static Type[] extractTypes(final Method method) {
    final Type[] parameterTypes = method.getGenericParameterTypes();
    final Type retType = extractReturnType(method);

    final Type[] res = new Type[parameterTypes.length + 1];
    System.arraycopy(parameterTypes, 0, res, 0, parameterTypes.length);
    res[parameterTypes.length] = retType;

    return res;
  }

  public static Type extractReturnType(final Method method) {
    Type retType = method.getGenericReturnType();
    if (retType instanceof ParameterizedType) {
      retType = ((ParameterizedType) retType).getActualTypeArguments()[0];
    }

    return retType;
  }
}
