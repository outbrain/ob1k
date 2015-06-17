package com.outbrain.ob1k.security.server;

import org.joda.time.DateTime;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class AuthenticationCookieTest {

  @Test
  public void testDelimitedString() {
    final AuthenticationCookie original = new AuthenticationCookie("username", DateTime.now(), "app-id", "realm");
    final AuthenticationCookie result = AuthenticationCookie.fromDelimitedString(original.toDelimitedString());
    assertEquals(original, result);
  }

}
