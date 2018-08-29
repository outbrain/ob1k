package com.outbrain.ob1k.crud.example;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.outbrain.ob1k.crud.dao.ICrudDao;
import com.outbrain.ob1k.crud.model.Entities;
import kotlin.Pair;
import kotlin.ranges.IntRange;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
  public Entities<Person> list(@NotNull IntRange pagination, @NotNull Pair<String, String> sort, @NotNull JsonObject filter) {

    Comparator<Person> comparator = Comparator.comparing(person -> getValue(person, sort.getFirst()));
    if (sort.getSecond().contains("DESC")) {
      comparator = comparator.reversed();
    }
    Integer startInteger = pagination.getStart();
    Integer endInteger = pagination.getEndInclusive();
    List<Person> list = map.values()
            .stream()
            .filter(c -> pass(c, filter))
            .skip(startInteger)
            .limit(endInteger - startInteger + 1)
            .sorted(comparator)
            .map(this::joinPersonWithJobs)
            .collect(Collectors.toList());
    return new Entities<>(map.size(), list);
  }

  private Person joinPersonWithJobs(Person person) {
    if (person != null) {
      person.setJobs(getJobs(person));
    }
    return person;
  }

  private List<Integer> getJobs(Person person) {
    return jobDao.hackGetAll().stream().filter(job -> job.getPerson().equals(person.getId())).map(Job::getId).collect(Collectors.toList());
  }

  @SuppressWarnings("unchecked")
  private <T> T getValue(Person c, String name) {
    try {
      Field field = Person.class.getDeclaredField(name.replace("\"", ""));
      field.setAccessible(true);
      return (T) field.get(c);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      return null;
    }
  }

  private boolean pass(Person c, @NotNull JsonObject filter) {
    return filter.entrySet().stream().allMatch(entry -> {
      String name = entry.getKey();
      Comparable myValue = getValue(c, name);
      String value = entry.getValue().toString().replace("\"", "");
      if (value.startsWith("[") && value.endsWith("]")) {
        return myValue == null || value.contains(myValue.toString());
      }
      return myValue == null || myValue.toString().contains(value);
    });
  }

  @Override
  public Person read(String id) {
    return joinPersonWithJobs(map.get(Integer.parseInt(id)));
  }

  @NotNull
  @Override
  public Person create(Person entity) {
    entity.setId(map.size() + 1);
    if (entity.getJobs() == null) {
      entity.setJobs(Lists.newArrayList());
    }
    map.put(entity.getId(), entity);
    return entity;
  }

  @NotNull
  @Override
  public Person update(String id, Person entity) {
    map.put(Integer.parseInt(id), joinPersonWithJobs(entity));
    return entity;
  }

  @NotNull
  @Override
  public int delete(String id) {
    Person person = map.get(Integer.parseInt(id));
    if (person != null && getJobs(person).size() > 0) {
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
