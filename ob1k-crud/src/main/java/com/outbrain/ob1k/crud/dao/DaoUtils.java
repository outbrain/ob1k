package com.outbrain.ob1k.crud.dao;

import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DaoUtils {


  /**
   * Java friendly spin off to the already existing CrudOperationsKt utilities
   */
  @NotNull
  public static <T> Entities<T> withSortFilterPaging(@NotNull Collection<T> values,
                                                     @NotNull IntRange pagination,
                                                     @NotNull Pair<String, String> sort,
                                                     T filter) {
    if (values.isEmpty()) {
      return new Entities<>(0, Collections.emptyList());
    }
    T first = values.iterator().next();
    List<T> sorted = CrudOperationsKt.sort(values, first.getClass(), sort);
    List<T> filtered = CrudOperationsKt.like(sorted, filter);
    List<T> limited = CrudOperationsKt.range(filtered, pagination);
    return new Entities<>(values.size(), limited);
  }
}
