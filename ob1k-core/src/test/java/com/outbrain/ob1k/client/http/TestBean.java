package com.outbrain.ob1k.client.http;

import java.util.List;

/**
 * User: aronen
 * Date: 8/18/13
 * Time: 8:14 PM
 */
public class TestBean {
  private String name;
  private int age;
  private List<String> habits;

  public TestBean() {}

  public TestBean(String name, int age, List<String> habits) {
    this.name = name;
    this.age = age;
    this.habits = habits;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public List<String> getHabits() {
    return habits;
  }

  public void setHabits(List<String> habits) {
    this.habits = habits;
  }
}
