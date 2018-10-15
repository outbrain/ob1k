package com.outbrain.ob1k.security.server;

import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.concurrent.ComposableFuture;
import com.outbrain.ob1k.concurrent.ComposableFutures;
import com.outbrain.ob1k.security.server.HttpBasicAuthenticationFilter.HttpBasicAccessAuthenticator;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpBasicAccessAuthenticatorTest {

  public static final String APP_ID = "myApp";
  private HttpBasicAccessAuthenticator basicAccessAuthenticator;
  private CredentialsAuthenticator<UserPasswordToken> credentialsAuthenticator;
  private AuthenticationCookieEncryptor cookieEncryptor;
  private Request request;
  private final int sessionMaxTimeSeconds = 10;
  private final static String ROOT = "root";


  @Before
  public void setup() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
    //An AES cookie encryptor
    final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    cookieEncryptor = new AuthenticationCookieAesEncryptor(keyGenerator.generateKey().getEncoded());

    //A credentials basicAccessAuthenticator for UserPassword credentials
    credentialsAuthenticator = new SpecificUserPasswordAuthenticator(ROOT, ROOT);

    //The basicAccessAuthenticator to test
    basicAccessAuthenticator = new HttpBasicAccessAuthenticator(
      cookieEncryptor,
      Collections.singletonList(credentialsAuthenticator),
      APP_ID,
      sessionMaxTimeSeconds);

    request = mock(Request.class);
    when(request.getPath()).thenReturn("/path");
  }

  @After
  public void tearDown() {
    basicAccessAuthenticator = null;
    credentialsAuthenticator = null;
    cookieEncryptor = null;
    request = null;
  }

  @Test
  public void testValidCookie() throws ExecutionException, InterruptedException {
    final String cookie = createCookie("username", DateTime.now(), APP_ID, credentialsAuthenticator.getId());
    setRequestCookie(cookie);
    assertEquals(credentialsAuthenticator.getId(), basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  //Tests an invalid cookie, meaning the cookie header will contain something that's not a cookie
  public void testInvalidCookie() throws ExecutionException, InterruptedException {
    setRequestCookie("not a cookie");
    assertNull(basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  public void testExpiredCookie() throws ExecutionException, InterruptedException {
    final DateTime cookieExpiredTime = DateTime.now().minusSeconds(sessionMaxTimeSeconds * 2);
    final String cookie = createCookie("username", cookieExpiredTime, "appId", credentialsAuthenticator.getId());
    setRequestCookie(cookie);
    assertNull(basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  public void testCookieWithWrongAppId() throws ExecutionException, InterruptedException {
    final String cookie = createCookie("username", DateTime.now(), "wrong_app_id", credentialsAuthenticator.getId());
    setRequestCookie(cookie);
    assertNull(credentialsAuthenticator.getId(), basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  public void testNoCookieAndNoCredentials() throws ExecutionException, InterruptedException {
    assertNull(basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  public void testInvalidCredentials() throws ExecutionException, InterruptedException {
    populateRequestWithCredentials("username", "expectedPassword");
    assertNull(basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  //Tests that the root URI is authenticated by rootAuthenticator
  public void testValidCredentials() throws ExecutionException, InterruptedException {
    populateRequestWithCredentials(ROOT, ROOT);
    assertEquals(credentialsAuthenticator.getId(), basicAccessAuthenticator.authenticate(request).get());
  }

  @Test
  //Tests that non-root URI is still authenticated by rootAuthenticator
  public void testValidCredentialsNonRootPath() throws ExecutionException, InterruptedException {
    when(request.getPath()).thenReturn("/not-associated-with-authenticator");
    populateRequestWithCredentials(ROOT, ROOT);
    assertEquals(credentialsAuthenticator.getId(), basicAccessAuthenticator.authenticate(request).get());
  }

  private void setRequestCookie(final String cookie) {
    when(request.getCookie(HttpBasicAccessAuthenticator.SESSION_COOKIE_NAME)).thenReturn(cookie);
  }

  private String createCookie(final String username,
                              final DateTime cookieExpiredTime,
                              final String appId,
                              final String authenticatorId) {
    final AuthenticationCookie cookie = new AuthenticationCookie(username,
                                                                 cookieExpiredTime,
                                                                 appId,
                                                                 authenticatorId);
    return cookieEncryptor.encrypt(cookie);
  }

  private void populateRequestWithCredentials(final String username, final String password) {
    final String authHeader = "Authorization";
    final String credentials = username + ":" + password;
    final String encodedCredentials = new String(Base64.getEncoder().encode(credentials.getBytes()));
    final String headerValue = "Basic " + encodedCredentials;
    when(request.getHeader(authHeader)).thenReturn(headerValue);
  }

  private static class SpecificUserPasswordAuthenticator implements CredentialsAuthenticator<UserPasswordToken> {
    private final String id = UUID.randomUUID().toString();
    private final String expectedUsername;
    private final String expectedPassword;

    public SpecificUserPasswordAuthenticator(final String expectedUsername, final String expectedPassword) {
      this.expectedUsername = expectedUsername;
      this.expectedPassword = expectedPassword;
    }

    @Override
    public ComposableFuture<Boolean> authenticate(final Credentials<UserPasswordToken> credentials) {
      final String password = new String(credentials.get().getPassword());
      final String username = credentials.get().getUsername();
      return ComposableFutures.fromValue(password.equals(expectedPassword) && username.equals(expectedUsername));
    }

    @Override
    public String getId() {
      return id;
    }
  }
}
