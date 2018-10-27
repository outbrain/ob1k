package com.outbrain.ob1k.crud.example;

import java.util.List;

public class Person {
  private Integer id;
  private String name;
  private Boolean alive;
  private String email;
  private ELiveness liveness;
  private List<Address> addresses;

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

  public List<Address> getAddresses() {
    return addresses;
  }

  public void setAddresses(List<Address> addresses) {
    this.addresses = addresses;
  }
}
