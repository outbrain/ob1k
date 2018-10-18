package com.outbrain.ob1k.http.common;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
                      .add("name", name)
                      .add("value", value)
                      .toString();
  }
}
