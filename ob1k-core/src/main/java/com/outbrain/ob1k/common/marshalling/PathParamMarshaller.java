package com.outbrain.ob1k.common.marshalling;

import javax.activation.UnsupportedDataTypeException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author marenzon
 */
public class PathParamMarshaller {

  private final static Map<Class<?>, Marshaller<?>> marshallers = new HashMap<Class<?>, Marshaller<?>>() {{

    put(String.class, new Marshaller<String>() {
      @Override
      public String unMarshell(final Object value) {
        return String.valueOf(value);
      }
    });

    put(Integer.class, new Marshaller<Integer>() {
      @Override
      public Integer unMarshell(final Object value) {
        return Integer.parseInt(String.valueOf(value));
      }
    });

    put(Boolean.class, new Marshaller<Boolean>() {
      @Override
      public Boolean unMarshell(final Object value) {
        return Boolean.parseBoolean(String.valueOf(value));
      }
    });

    put(Long.class, new Marshaller<Long>() {
      @Override
      public Long unMarshell(final Object value) {
        return Long.parseLong(String.valueOf(value));
      }
    });

    put(Float.class, new Marshaller<Float>() {
      @Override
      public Float unMarshell(final Object value) {
        return Float.parseFloat(String.valueOf(value));
      }
    });

    put(Double.class, new Marshaller<Double>() {
      @Override
      public Double unMarshell(final Object value) {
        return Double.parseDouble(String.valueOf(value));
      }
    });

    put(Character.class, new Marshaller<Character>() {
      @Override
      public Character unMarshell(final Object value) {
        return String.valueOf(value).charAt(0);
      }
    });

    put(Byte.class, new Marshaller<Byte>() {
      @Override
      public Byte unMarshell(final Object value) {
        return Byte.parseByte(String.valueOf(value));
      }
    });

    put(Short.class, new Marshaller<Short>() {
      @Override
      public Short unMarshell(final Object value) {
        return Short.parseShort(String.valueOf(value));
      }
    });

  }};

  public static <T> T unMarshell(final Object value, final Class<T> type) throws UnsupportedDataTypeException {

    final Marshaller<?> marshaller = marshallers.get(type);

    if (marshaller == null) {
      throw new UnsupportedDataTypeException("Can't unmarshell type " + type.getName());
    }

    return (T) marshaller.unMarshell(value);
  }

  private interface Marshaller<T> {
    T unMarshell(Object value);
  }
}