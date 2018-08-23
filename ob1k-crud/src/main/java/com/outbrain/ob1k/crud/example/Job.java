package com.outbrain.ob1k.crud.example;

import java.util.Date;

public class Job {
  private Integer id;
  private String company;
  private String title;
  private Date timestamp;
  private Integer person;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getPerson() {
    return person;
  }

  public void setPerson(Integer person) {
    this.person = person;
  }
}
