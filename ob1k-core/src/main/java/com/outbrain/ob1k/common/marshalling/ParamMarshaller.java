package com.outbrain.ob1k.common.marshalling;

import javax.activation.UnsupportedDataTypeException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author marenzon
 */
public class ParamMarshaller {

  private final static Map<Class<?>, Marshaller<?>> marshallers = new HashMap<Class<?>, Marshaller<?>>() {{

    put(String.class, new Marshaller<String>() {
      @Override
      public String unmarshall(final Object value) {
        return String.valueOf(value);
      }
    });

    put(int.class, new Marshaller<Integer>() {
      @Override
      public Integer unmarshall(final Object value) {
        return Integer.parseInt(String.valueOf(value));
      }
    });

    put(boolean.class, new Marshaller<Boolean>() {
      @Override
      public Boolean unmarshall(final Object value) {
        return Boolean.parseBoolean(String.valueOf(value));
      }
    });

    put(long.class, new Marshaller<Long>() {
      @Override
      public Long unmarshall(final Object value) {
        return Long.parseLong(String.valueOf(value));
      }
    });

    put(float.class, new Marshaller<Float>() {
      @Override
      public Float unmarshall(final Object value) {
        return Float.parseFloat(String.valueOf(value));
      }
    });

    put(double.class, new Marshaller<Double>() {
      @Override
      public Double unmarshall(final Object value) {
        return Double.parseDouble(String.valueOf(value));
      }
    });

    put(char.class, new Marshaller<Character>() {
      @Override
      public Character unmarshall(final Object value) {
        return String.valueOf(value).charAt(0);
      }
    });

    put(byte.class, new Marshaller<Byte>() {
      @Override
      public Byte unmarshall(final Object value) {
        return Byte.parseByte(String.valueOf(value));
      }
    });

    put(short.class, new Marshaller<Short>() {
      @Override
      public Short unmarshall(final Object value) {
        return Short.parseShort(String.valueOf(value));
      }
    });
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