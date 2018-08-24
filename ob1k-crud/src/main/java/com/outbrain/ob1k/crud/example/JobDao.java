package com.outbrain.ob1k.crud.example;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.crud.dao.ICrudAsyncDao;
import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JobDao implements ICrudAsyncDao<Job> {
  private final Map<Integer, Job> map = Maps.newHashMap();

  Collection<Job> hackGetAll() {
    return map.values();
  }

  @NotNull
  @Override
  @SuppressWarnings("Unchecked")
  public ComposableFuture<Entities<Job>> list(@NotNull IntRange pagination, @NotNull Pair<String, String> sort, @NotNull JsonObject filter) {
    Comparator<Job> comparator = Comparator.comparing(company -> getValue(company, sort.getFirst()));
    if (sort.getSecond().contains("DESC")) {
      comparator = comparator.reversed();
    }
    Integer endInteger = pagination.getEndInclusive();
    Integer startInteger = pagination.getStart();
    List<Job> list = map.values()
            .stream()
            .filter(c -> pass(c, filter))
            .skip(startInteger)
            .limit(endInteger - startInteger + 1)
            .sorted(comparator)
            .collect(Collectors.toList());
    return ComposableFutures.fromValue(new Entities<>(map.size(), list));
  }

  private Comparable getValue(Job c, String name) {
    try {
      Field field = Job.class.getDeclaredField(name.replace("\"", ""));
      field.setAccessible(true);
      return (Comparable) field.get(c);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

  private boolean pass(Job c, @NotNull JsonObject filter) {
    return filter.entrySet().stream().allMatch(entry -> {
      String name = entry.getKey();
      String value = entry.getValue().toString().replace("\"", "");
      Comparable myValue = getValue(c, name);
      if(value.startsWith("[") && value.endsWith("]")){
        return myValue == null || value.contains(myValue.toString());
      }
      return myValue == null || myValue.toString().contains(value);
    });
  }

  @NotNull
  @Override
  public ComposableFuture<Job> read(int id) {
    return ComposableFutures.fromValue(map.get(id));
  }

  @NotNull
  @Override
  public ComposableFuture<Job> create(Job entity) {
    entity.setId(map.size() + 1);
    map.put(entity.getId(), entity);
    return ComposableFutures.fromValue(entity);
  }

  @NotNull
  @Override
  public ComposableFuture<Job> update(int id, Job entity) {
    map.put(id, entity);
    return ComposableFutures.fromValue(entity);
  }

  @NotNull
  @Override
  public ComposableFuture<Integer> delete(int id) {
    return ComposableFutures.fromValue(map.remove(id) == null ? 0 : 1);
  }

  @NotNull
  @Override
  public String resourceName() {
    return "job";
  }

  @NotNull
  @Override
  public Class<Job> type() {
    return Job.class;
  }
}
