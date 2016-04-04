package com.outbrain.ob1k.cache.memcache.folsom;

/**
 * @author Eran Harel
 */
class Person {
  public int age;
  public String name;
  public double weight;

  public Person() {
  }

  public Person(final int age, final String name, final double weight) {
    this.age = age;
    this.name = name;
    this.weight = weight;
  }
}
