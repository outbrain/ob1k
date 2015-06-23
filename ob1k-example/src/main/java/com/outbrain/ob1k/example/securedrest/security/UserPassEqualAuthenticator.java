package com.outbrain.ob1k.example.securedrest.security;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.security.server.Credentials;
import com.outbrain.ob1k.security.server.CredentialsAuthenticator;
import com.outbrain.ob1k.security.server.UserPasswordToken;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by gmarom on 6/22/15
 */
public class UserPassEqualAuthenticator implements CredentialsAuthenticator<UserPasswordToken> {

  /**
   * Authenticates the given credentials by comparing the username and password
   *
   * @return true if the username is equal to the password
   */
  @Override
  public ComposableFuture<Boolean> authenticate(final Credentials<UserPasswordToken> credentials) {
    String user = credentials.get().getUsername();
    String pass = new String(credentials.get().getPassword());
    return ComposableFutures.fromValue(StringUtils.equals(user, pass));
  }

  /**
   * Returns this CredentialAuthenticator's class name in simple form
   * <p>
   *   Returning the class' name is only OK if there will be only one instance of this authenticator.
   *   <b>Multiple instances of CredentialAuthenticators must return different IDs</b>
   * </p>
   */
  @Override
  public String getId() {
    return UserPassEqualAuthenticator.class.getSimpleName();
  }
}
