package com.outbrain.ob1k.http.common;

/**
 * Represents cookie for the request
 *
 * @author marenzon
 */
public class Cookie {

  private final String name;
  private final String value;
  private final String domain;
  private final String path;
  private final long maxAge;
  private final boolean isSecure;
  private final boolean isHttpOnly;

  public Cookie(final String name, final String value, final String domain, final String path, final long maxAge,
                final boolean isSecure, final boolean isHttpOnly) {

    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.maxAge = maxAge;
    this.isSecure = isSecure;
    this.isHttpOnly = isHttpOnly;
  }

  public String getName() {

    return name;
  }

  public String getValue() {

    return value;
  }

  public String getDomain() {

    return domain;
  }

  public String getPath() {

    return path;
  }

  public long getMaxAge() {

    return maxAge;
  }

  public boolean isSecure() {

    return isSecure;
  }

  public boolean isHttpOnly() {

    return isHttpOnly;
  }
}