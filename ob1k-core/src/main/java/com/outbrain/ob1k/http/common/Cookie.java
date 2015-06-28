package com.outbrain.ob1k.http.common;

/**
 * @author marenzon
 */
public class Cookie {

  private final String name;
  private final String value;
  private final String domain;
  private final String path;
  private final int maxAge;
  private final boolean secure;
  private final boolean httpOnly;

  public Cookie(final String name, final String value, final String domain, final String path, final int maxAge,
                final boolean secure, final boolean httpOnly) {

    this.name = name;
    this.value = value;
    this.domain = domain;
    this.path = path;
    this.maxAge = maxAge;
    this.secure = secure;
    this.httpOnly = httpOnly;
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

  public int getMaxAge() {

    return maxAge;
  }

  public boolean isSecure() {

    return secure;
  }

  public boolean isHttpOnly() {

    return httpOnly;
  }
}