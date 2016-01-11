package com.outbrain.ob1k.security.server;

import org.joda.time.DateTime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AuthenticationCookieTest {

  @Test
  public void testDelimitedString() {
    final AuthenticationCookie original = new AuthenticationCookie("username", DateTime.now(), "app-id", "realm");
    final AuthenticationCookie result = AuthenticationCookie.fromDelimitedString(original.toDelimitedString());
    assertEquals(original.getAppId(), result.getAppId());
    assertEquals(original.getAuthenticatorId(), result.getAuthenticatorId());
    assertEquals(original.getCreationTime(), result.getCreationTime());
    assertEquals(original.getUsername(), result.getUsername());
  }

}
