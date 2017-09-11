package com.outbrain.ob1k.server.services;

import com.outbrain.ob1k.Service;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import java.util.List;

/**
 * @author marenzon
 */
public interface RequestsTestService extends Service {

  class Person {
    public int id;
    public String name;
    public String profession;

    public Person() {

    }

    public Person(final int id, final String name, final String profession) {
      this.id = id;
      this.name = name;
      this.profession = profession;
    }

    public int getId() {
      return id;
    }

    public void setId(final int id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getProfession() {
      return profession;
    }

    public void setProfession(final String profession) {
      this.profession = profession;
    }
  }

  ComposableFuture<List<Person>> getAll();
  ComposableFuture<Person> fetchUser(int id);
  ComposableFuture<String> updateUser(int id, String name, String profession);
  ComposableFuture<String> deleteUser(int id);
  ComposableFuture<String> optionsUser(int id);
  ComposableFuture<Person> createUser(String name, String profession);
  ComposableFuture<String> printDetails(String firstName, String lastName, int age);
}
