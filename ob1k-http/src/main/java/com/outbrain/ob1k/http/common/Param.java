package com.outbrain.ob1k.http.common;

import com.google.common.base.Objects;

/**
 * @author marenzon
 */
public class Param {

  private final String name;
  private final String value;

  public Param(final String name, final String value) {

    this.name = name;
    this.value = value;
  }

  public String getName() {

    return name;
  }

  public String getValue() {

    return value;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
            .add("name", name)
            .add("value", value)
            .toString();
  }
}