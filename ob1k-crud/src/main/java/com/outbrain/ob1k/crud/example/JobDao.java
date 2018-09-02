package com.outbrain.ob1k.crud.example;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.crud.dao.DaoUtils;
import com.outbrain.ob1k.crud.dao.ICrudDao;
import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;

/**
 * example of the crud dao implementation
 */
public class JobDao implements ICrudDao<Job> {
  private final Map<Integer, Job> map = Maps.newHashMap();

  Collection<Job> hackGetAll() {
    return map.values();
  }

  @NotNull
  @Override
  public Entities<Job> list(@NotNull IntRange pagination, @NotNull Pair<String, String> sort, Job filter) {
    return DaoUtils.withSortFilterPaging(map.values(), pagination, sort, filter);
  }

  @Override
  public Job read(String id) {
    return map.get(Integer.parseInt(id));
  }

  @NotNull
  @Override
  public Job create(Job entity) {
    entity.setId(map.size() + 1);
    map.put(entity.getId(), entity);
    return entity;
  }

  @NotNull
  @Override
  public Job update(String id, Job entity) {
    map.put(Integer.parseInt(id), entity);
    return entity;
  }

  @NotNull
  @Override
  public int delete(String id) {
    return map.remove(Integer.parseInt(id)) == null ? 0 : 1;
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
