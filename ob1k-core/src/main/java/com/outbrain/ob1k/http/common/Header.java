package com.outbrain.ob1k.http.common;

/**
 * @author marenzon
 */
public class Header {

  private final String name;
  private final String value;

  public Header(final String name, final String value) {

    this.name = name;
    this.value = value;
  }

  public String getName() {

    return name;
  }

  public String getValue() {

    return value;
  }
}