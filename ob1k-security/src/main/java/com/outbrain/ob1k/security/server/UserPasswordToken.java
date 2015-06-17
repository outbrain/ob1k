package com.outbrain.ob1k.security.server;

public class UserPasswordToken {

  private final String username;
  private final char[] password;

  public UserPasswordToken(final String username, final char[] password) {
    this.password = password;
    this.username = username;
  }

  public String getUsername() {
    return username;
  }

  public char[] getPassword() {
    return password;
  }
}
