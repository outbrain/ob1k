package com.outbrain.ob1k.crud.example;

import java.util.List;

public class Person {
  private Integer id;
  private String name;
  private Boolean alive;
  private String email;
  private List<Integer> jobs;

  public List<Integer> getJobs() {
    return jobs;
  }

  public void setJobs(List<Integer> jobs) {
    this.jobs = jobs;
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Boolean getAlive() {
    return alive;
  }

  public void setAlive(Boolean alive) {
    this.alive = alive;
  }
}
