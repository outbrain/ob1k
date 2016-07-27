package com.outbrain.ob1k.common.marshalling;

import javax.activation.UnsupportedDataTypeException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author marenzon
 */
public class ParamMarshaller {

  private final static Map<Class<?>, Marshaller<?>> marshallers = new HashMap<Class<?>, Marshaller<?>>() {{
    put(String.class, String::valueOf);
    put(int.class, value -> Integer.parseInt(String.valueOf(value)));
    put(boolean.class, value -> Boolean.parseBoolean(String.valueOf(value)));
    put(long.class, value -> Long.parseLong(String.valueOf(value)));
    put(float.class, value -> Float.parseFloat(String.valueOf(value)));
    put(double.class, value -> Double.parseDouble(String.valueOf(value)));
    put(char.class, value -> String.valueOf(value).charAt(0));
    put(byte.class, value -> Byte.parseByte(String.valueOf(value)));
    put(short.class, value -> Short.parseShort(String.valueOf(value)));
  }};

  @SuppressWarnings("unchecked")
  public static <T> T unmarshall(final Object value, final Class<T> type) throws UnsupportedDataTypeException {

    final Marshaller<?> marshaller = marshallers.get(type);

    if (marshaller == null) {
      throw new UnsupportedDataTypeException("can't unmarshall type " + type.getName());
    }

    return (T) marshaller.unmarshall(value);
  }

  private interface Marshaller<T> {

    T unmarshall(Object value);
  }
}