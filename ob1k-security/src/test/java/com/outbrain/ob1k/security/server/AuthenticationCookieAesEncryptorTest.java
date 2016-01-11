package com.outbrain.ob1k.security.server;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;

public class AuthenticationCookieAesEncryptorTest {

  private AuthenticationCookieAesEncryptor encryptor;

  @Before
  public void setup() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
    final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    encryptor = new AuthenticationCookieAesEncryptor(keyGenerator.generateKey().getEncoded());
  }

  @After
  public void tearDown() {
    encryptor = null;
  }

  @Test
  public void testEncryptor() {
    final AuthenticationCookie original = new AuthenticationCookie("username", DateTime.now(), "appId", "realm");
    final AuthenticationCookie result = encryptor.decrypt(encryptor.encrypt(original));

    assertEquals(original.toDelimitedString(), result.toDelimitedString());
  }

}
