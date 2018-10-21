package com.outbrain.ob1k.example.rest.api;


import java.util.Objects;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Simple pojo which describes a User entity for the example
 *
 * @author marenzon
 */
public class User {

  private int id;
  private String name;
  private String address;
  private String profession;

  // Empty constructor for the marshallers
  public User() {}

  public User(final String name, final String address, final String profession) {
    this.name = Objects.requireNonNull(name, "name may not be null");
    this.address = Objects.requireNonNull(address, "address may not be null");
    this.profession = Objects.requireNonNull(profession, "profession may not be null");
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

  public String getAddress() {
    return address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public String getProfession() {
    return profession;
  }

  public void setProfession(final String profession) {
    this.profession = profession;
  }

  public void updateFrom(final User userData) {
    Objects.requireNonNull(userData, "userData may not be null");

    if (userData.getName() != null) {
      setName(userData.getName());
    }

    if (userData.getAddress() != null) {
      setAddress(userData.getAddress());
    }

    if (userData.getProfession() != null) {
      setProfession(userData.getProfession());
    }
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
             .append("id", id)
             .append("name", name)
             .append("address", address)
             .append("profession", profession)
             .toString();
  }
}
