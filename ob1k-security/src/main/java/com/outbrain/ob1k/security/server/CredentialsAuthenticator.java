package com.outbrain.ob1k.security.server;

public interface CredentialsAuthenticator<T> {

  /**
   * Authenticates the given credentials.
   * It is left to the implementation to define the structure of the credentials object.
   *
   * @return {@code true} if authentication succeeded
   */
  boolean authenticate(Credentials<T> credentials);

  /**
   * Returns the unique identifier of this CredentialsAuthenticator.<br>
   * The return value must fulfill the following promises:
   * <ul>
   *   <li>Different implementations of the {@code CredentialsAuthenticator} must return a different id</li>
   *   <li>
   *     Different instances of the same {@code CredentialsAuthenticator} will return a different id
   *     iff their configuration is different
   *   </li>
   * </ul>
   */
  String getId();

}
