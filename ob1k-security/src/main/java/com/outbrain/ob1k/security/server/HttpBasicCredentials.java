package com.outbrain.ob1k.security.server;

class HttpBasicCredentials implements Credentials<UserPasswordToken> {

  private final UserPasswordToken credentials;

  public HttpBasicCredentials(final UserPasswordToken credentials) {
    this.credentials = credentials;
  }

  @Override
  public UserPasswordToken get() {
    return credentials;
  }
}
