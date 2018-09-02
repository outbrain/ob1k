package com.outbrain.ob1k.crud.example;

import com.google.common.collect.Maps;
import com.outbrain.ob1k.crud.dao.DaoUtils;
import com.outbrain.ob1k.crud.dao.ICrudDao;
import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * example of the async crud dao implementation
 */
public class PersonDao implements ICrudDao<Person> {
  private final Map<Integer, Person> map = Maps.newHashMap();
  private final JobDao jobDao;

  public PersonDao(JobDao jobDao) {
    this.jobDao = jobDao;
  }

  @NotNull
  @Override
  public Entities<Person> list(@NotNull IntRange pagination, @NotNull Pair<String, String> sort, Person filter) {
    return DaoUtils.withSortFilterPaging(map.values(), pagination, sort, filter);
  }

  @Override
  public Person read(String id) {
    return map.get(Integer.parseInt(id));
  }

  @NotNull
  @Override
  public Person create(Person entity) {
    entity.setId(map.size() + 1);
    map.put(entity.getId(), entity);
    return entity;
  }

  @NotNull
  @Override
  public Person update(String id, Person entity) {
    map.put(Integer.parseInt(id), entity);
    return entity;
  }

  @NotNull
  @Override
  public int delete(String id) {
    if (jobDao.hackGetAll().stream().anyMatch(job -> job.getPerson().toString().equals(id))) {
      throw new RuntimeException("can't delete person with jobs");
    }
    return map.remove(Integer.parseInt(id)) == null ? 0 : 1;
  }

  @NotNull
  @Override
  public String resourceName() {
    return "person";
  }

  @NotNull
  @Override
  public Class<Person> type() {
    return Person.class;
  }
}
