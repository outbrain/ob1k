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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PersonDao implements ICrudAsyncDao<Person> {
  private final Map<Integer, Person> map = Maps.newHashMap();
  private final JobDao jobDao;

  public PersonDao(JobDao jobDao) {
    this.jobDao = jobDao;
  }

  @NotNull
  @Override
  @SuppressWarnings("Unchecked")
  public ComposableFuture<Entities<Person>> list(@NotNull IntRange pagination, @NotNull Pair<String, String> sort, @NotNull JsonObject filter) {

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
    return ComposableFutures.fromValue(new Entities<>(map.size(), list));
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

  private Comparable getValue(Person c, String name) {
    try {
      Field field = Person.class.getDeclaredField(name.replace("\"", ""));
      field.setAccessible(true);
      return (Comparable) field.get(c);
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

  @NotNull
  @Override
  public ComposableFuture<Person> read(int id) {
    return ComposableFutures.fromValue(joinPersonWithJobs(map.get(id)));
  }

  @NotNull
  @Override
  public ComposableFuture<Person> create(Person entity) {
    entity.setId(map.size() + 1);
    map.put(entity.getId(), entity);
    return ComposableFutures.fromValue(entity);
  }

  @NotNull
  @Override
  public ComposableFuture<Person> update(int id, Person entity) {
    map.put(id, joinPersonWithJobs(entity));
    return ComposableFutures.fromValue(entity);
  }

  @NotNull
  @Override
  public ComposableFuture<Integer> delete(int id) {
    Person person = map.get(id);
    if (person != null && getJobs(person).size() > 0) {
      return ComposableFutures.fromError(new RuntimeException("can't delete person with jobs"));
    }
    return ComposableFutures.fromValue(map.remove(id) == null ? 0 : 1);
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
