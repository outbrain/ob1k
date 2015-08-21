package com.outbrain.ob1k.example.rest.api;

/**
 * Users Exception
 * Thrown in case of illegal actions on users service endpoints
 * @author marenzon
 */
public class UsersException extends Exception {

  public UsersException(final String message) {
    super(message);
  }
}
