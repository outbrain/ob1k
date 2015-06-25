package com.outbrain.ob1k.security.common;

import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.security.server.Credentials;
import com.outbrain.ob1k.security.server.CredentialsAuthenticator;
import com.outbrain.ob1k.security.server.UserPasswordToken;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by gmarom on 6/24/15
 */
public class UserPassEqualAuthenticator implements CredentialsAuthenticator<UserPasswordToken> {
  @Override
  public ComposableFuture<Boolean> authenticate(final Credentials<UserPasswordToken> credentials) {
    String user = credentials.get().getUsername();
    String pass = new String(credentials.get().getPassword());
    return ComposableFutures.fromValue(StringUtils.equals(user, pass));
  }

  @Override
  public String getId() {
    return UserPassEqualAuthenticator.class.getSimpleName();
  }
}
