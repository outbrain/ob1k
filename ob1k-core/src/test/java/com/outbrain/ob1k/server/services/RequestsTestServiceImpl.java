package com.outbrain.ob1k.server.services;

import com.google.common.collect.Lists;
import com.outbrain.ob1k.concurrent.ComposableFuture;

import java.util.List;

import static com.outbrain.ob1k.concurrent.ComposableFutures.fromValue;

/**
 * @author marenzon
 */
public class RequestsTestServiceImpl implements RequestsTestService {

  public final static String GREAT_SUCCESS = "Great success";

  private final Person yossuf = new Person(1, "Yossuf", "Pita baker");

  @Override
  public ComposableFuture<List<Person>> getAll() {
    final List<Person> persons = Lists.newArrayList(yossuf);
    return fromValue(persons);
  }

  @Override
  public ComposableFuture<Person> fetchUser(final int id) {
    return fromValue(yossuf);
  }

  @Override
  public ComposableFuture<String> updateUser(final int id, final String name, final String profession) {
    return fromValue(GREAT_SUCCESS);
  }

  @Override
  public ComposableFuture<String> deleteUser(final int id) {
    return fromValue(GREAT_SUCCESS);
  }

  @Override
  public ComposableFuture<Person> createUser(final String name, final String profession) {
    return fromValue(new Person(2, name, profession));
  }

  @Override
  public ComposableFuture<String> printDetails(final String firstName, final String lastName, final int age) {
    return fromValue(firstName + " " + lastName + " (" + age + ")");
  }

  @Override
  public ComposableFuture<String> optionsUser(int id) {
    return fromValue(id + " is optional");
  }
}
