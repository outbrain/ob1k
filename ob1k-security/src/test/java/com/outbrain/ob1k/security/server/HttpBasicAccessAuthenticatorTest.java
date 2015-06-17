package com.outbrain.ob1k.security.server;

import com.ning.http.util.Base64;
import com.outbrain.ob1k.Request;
import com.outbrain.ob1k.security.server.HttpBasicAuthenticationFilter.HttpBasicAccessAuthenticator;
import com.outbrain.ob1k.security.server.PathAssociations.PathAssociationsBuilder;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpBasicAccessAuthenticatorTest {

  public static final String APP_ID = "myApp";
  private HttpBasicAccessAuthenticator basicAccessAuthenticator;
  private CredentialsAuthenticator<UserPasswordToken> rootAuthenticator;
  private CredentialsAuthenticator<UserPasswordToken> johnAuthenticator;
  private CredentialsAuthenticator<UserPasswordToken> georgeAuthenticator;
  private AuthenticationCookieEncryptor cookieEncryptor;
  private Request request;
  private final int sessionMaxTimeSeconds = 10;
  private final static String ROOT = "root";
  private final static String JOHN = "john";
  private final static String GEORGE = "george";


  @Before
  public void setup() throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
    //An AES cookie encryptor
    final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
    cookieEncryptor = new AuthenticationCookieAesEncryptor(keyGenerator.generateKey().getEncoded());

    //A credentials basicAccessAuthenticator for UserPassword credentials
    rootAuthenticator = new SpecificUserPasswordAuthenticator(ROOT, ROOT);
    johnAuthenticator = new SpecificUserPasswordAuthenticator(JOHN, JOHN);
    georgeAuthenticator = new SpecificUserPasswordAuthenticator(GEORGE, GEORGE);

    //Path associations - the johnAuthenticator may authorize all URIs
    final PathAssociations<UserPasswordToken> associations = new PathAssociationsBuilder<UserPasswordToken>()
      .associate("/", rootAuthenticator)
      .associate("/" + JOHN, johnAuthenticator)
      .associate("/" + GEORGE, georgeAuthenticator)
      .build();

    //The basicAccessAuthenticator to test
    basicAccessAuthenticator = new HttpBasicAccessAuthenticator(
      cookieEncryptor,
      associations,
      APP_ID,
      sessionMaxTimeSeconds);

    request = mock(Request.class);
    when(request.getPath()).thenReturn("/path");
  }

  @After
  public void tearDown() {
    basicAccessAuthenticator = null;
    rootAuthenticator = null;
    johnAuthenticator = null;
    georgeAuthenticator = null;
    cookieEncryptor = null;
    request = null;
  }

  @Test
  public void testValidCookie() {
    final String cookie = createCookie("username", DateTime.now(), APP_ID, rootAuthenticator.getId());
    when(request.getHeader(HttpBasicAccessAuthenticator.SESSION_COOKIE_HEADER)).thenReturn(cookie);
    assertEquals(rootAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  @Test
  //Tests an invalid cookie, meaning the cookie header will contain something that's not a cookie
  public void testInvalidCookie() {
    when(request.getHeader(HttpBasicAccessAuthenticator.SESSION_COOKIE_HEADER)).thenReturn("not a cookie");
    assertNull(basicAccessAuthenticator.authenticate(request));
  }

  @Test
  public void testExpiredCookie() {
    final DateTime cookieExpiredTime = DateTime.now().minusSeconds(sessionMaxTimeSeconds * 2);
    final String cookie = createCookie("username", cookieExpiredTime, "appId", rootAuthenticator.getId());
    when(request.getHeader(HttpBasicAccessAuthenticator.SESSION_COOKIE_HEADER)).thenReturn(cookie);
    assertNull(basicAccessAuthenticator.authenticate(request));
  }

  @Test
  public void testCookieWithWrongAppId() {
    final String cookie = createCookie("username", DateTime.now(), "wrong_app_id", rootAuthenticator.getId());
    when(request.getHeader(HttpBasicAccessAuthenticator.SESSION_COOKIE_HEADER)).thenReturn(cookie);
    assertNull(rootAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  @Test
  public void testNoCookieAndNoCredentials() {
    assertNull(basicAccessAuthenticator.authenticate(request));
  }

  @Test
  public void testInvalidCredentials() {
    populateRequestWithCredentials("username", "expectedPassword");
    assertNull(basicAccessAuthenticator.authenticate(request));
  }

  @Test
  //Tests that the root URI is authenticated by rootAuthenticator
  public void testValidCredentials() {
    populateRequestWithCredentials(ROOT, ROOT);
    assertEquals(rootAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  @Test
  //Tests that non-root URI is still authenticated by rootAuthenticator
  public void testValidCredentialsNonRootPath() {
    when(request.getPath()).thenReturn("/not-associated-with-authenticator");
    populateRequestWithCredentials(ROOT, ROOT);
    assertEquals(rootAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  @Test
  //Tests that a URI under /john is authenticated by johnAuthenticator when provided with JOHN credentials
  public void testJohnUriWithJohnCredentials() {
    when(request.getPath()).thenReturn(JOHN + "/some-suffix");
    populateRequestWithCredentials(JOHN, JOHN);
    assertEquals(johnAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  @Test
  //Tests that a URI under /john is authenticated by rootAuthenticator when provided with ROOT credentials
  public void testJohnUriWithRootCredentials() {
    when(request.getPath()).thenReturn(JOHN + "/some-suffix");
    populateRequestWithCredentials(ROOT, ROOT);
    assertEquals(rootAuthenticator.getId(), basicAccessAuthenticator.authenticate(request));
  }

  private String createCookie(final String username, final DateTime cookieExpiredTime,
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
    final String encodedCredentials = Base64.encode(credentials.getBytes());
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
    public boolean authenticate(final Credentials<UserPasswordToken> credentials) {
      final String password = new String(credentials.get().getPassword());
      final String username = credentials.get().getUsername();
      return password.equals(expectedPassword) && username.equals(expectedUsername);
    }

    @Override
    public String getId() {
      return id;
    }
  }
}
