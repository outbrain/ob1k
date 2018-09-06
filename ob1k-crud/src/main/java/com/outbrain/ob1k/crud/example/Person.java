package com.outbrain.ob1k.crud.example;

public class Person {
  private Integer id;
  private String name;
  private Boolean alive;
  private String email;
  private ELiveness liveness;

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

  public ELiveness getLiveness() {
    return liveness;
  }

  public void setLiveness(ELiveness liveness) {
    this.liveness = liveness;
  }
}
