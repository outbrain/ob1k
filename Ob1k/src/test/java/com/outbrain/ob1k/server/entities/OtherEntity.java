package com.outbrain.ob1k.server.entities;

/**
 * Created by aronen on 10/6/14.
 */
public class OtherEntity {
  private int value1;
  private String value2;

  public OtherEntity() {}

  public OtherEntity(int value1, String value2) {
    this.value1 = value1;
    this.value2 = value2;
  }

  public int getValue1() {
    return value1;
  }

  public void setValue1(int value1) {
    this.value1 = value1;
  }

  public String getValue2() {
    return value2;
  }

  public void setValue2(String value2) {
    this.value2 = value2;
  }
}
