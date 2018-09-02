package com.outbrain.ob1k.crud.dao;

import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DaoUtils {


  @NotNull
  public static <T> Entities<T> withSortFilterPaging(@NotNull Collection<T> values,
                                                     @NotNull IntRange pagination,
                                                     @NotNull Pair<String, String> sort,
                                                     T filter) {
    Integer startInteger = pagination.getStart();
    Integer endInteger = pagination.getEndInclusive();

    Comparator<T> comparator = getComparator(sort);
    List<T> list = values.stream()
            .sorted(comparator)
            .filter(c -> like(c, filter))
            .skip(startInteger)
            .limit(endInteger - startInteger + 1)
            .collect(Collectors.toList());
    return new Entities<>(values.size(), list);
  }

  public static <T> boolean like(T c, T like) {
    if (like == null) {
      return true;
    }
    return properties(c)
            .allMatch(pd -> {
              Object filterValue = read(pd, like);
              Object myValue = read(pd, c);
              if (filterValue == null) {
                return true;
              }
              if (myValue == null) {
                return false;
              }
              if (filterValue instanceof String) {
                return myValue.toString().contains(filterValue.toString());
              } else {
                return myValue.equals(filterValue);
              }
            });
  }


  public static <T> Comparator<T> getComparator(Pair<String, String> sort) {
    Comparator<T> comparator = Comparator.comparing(company -> getValue(company, sort.getFirst()));
    return sort.getSecond().contains("DESC") ? comparator.reversed() : comparator;
  }

  @SuppressWarnings("unchecked")
  private static <X, Y> Y getValue(X c, String name) {
    return (Y) properties(c)
            .filter(pd -> pd.getName().equals(name))
            .map(pd -> read(pd, c)).findFirst().orElseThrow(() -> new IllegalArgumentException("failed to find  property " + name));
  }

  @NotNull
  private static <X> Stream<PropertyDescriptor> properties(X c) {
    try {
      return Stream.of(Introspector.getBeanInfo(c.getClass()).getPropertyDescriptors());
    } catch (IntrospectionException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  private static Object read(PropertyDescriptor pd, Object value) {
    try {
      return pd.getReadMethod().invoke(value);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
